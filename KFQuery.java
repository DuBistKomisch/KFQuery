import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

class KFQuery
{
  private static final int PORT = 28852;
  private static final byte[] A2S_INFO =
  {
    (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
    0x54,
    0x53, 0x6F, 0x75, 0x72, 0x63, 0x65, 0x20, 0x45, 0x6E, 0x67, 0x69, 0x6E, 0x65, 0x20, 0x51, 0x75, 0x65, 0x72, 0x79,
    0x00
  };

  private static int lines = 0;

  public static void main (String[] args)
  {
    System.out.print("[");

    BufferedReader reader = null;
    try
    {
      reader = new BufferedReader(new FileReader("servers.list"));
      String address = null;
      while ((address = reader.readLine()) != null)
      {
        // connect and send
        DatagramSocket socket = null;
        InetSocketAddress dest = new InetSocketAddress(address, KFQuery.PORT);
        DatagramPacket request = new DatagramPacket(KFQuery.A2S_INFO, 25, dest);
        try
        {
          socket = new DatagramSocket();
          socket.setSoTimeout(5000);
          socket.connect(dest);
          socket.send(request);
        }
        catch (SocketException e)
        {
          output();
          if (socket != null)
            socket.close();
          continue;
        }

        // receive
        byte[] buffer = new byte[256];
        DatagramPacket response = new DatagramPacket(buffer, 256);
        try
        {
          socket.receive(response);
        }
        catch (SocketException | SocketTimeoutException e)
        {
          output();
          continue;
        }
        finally
        {
          socket.close();
        }

        // process
        ServerInfo info = new ServerInfo(buffer);
        output(info.players, info.map);
      }
    }
    catch (IOException e)
    {
    }

    System.out.println("]");
  }

  private static void output()
  {
    KFQuery.output(-1, "");
  }

  private static void output(int players, String map)
  {
    System.out.printf((lines++ > 0 ? "," : "") + "[%d,\"%s\"]", players, map);
  }

  private static class ServerInfo
  {
    public byte header, protocol, players, maxPlayers, bots, type, environment, visibility, vac;
    public short id;
    public String name, map, folder, game, version;

    ServerInfo(byte[] data)
    {
      ByteBuffer buffer = ByteBuffer.wrap(data);
      buffer.get();
      buffer.get();
      buffer.get();
      buffer.get();
      header = buffer.get();
      protocol = buffer.get();
      name = getString(buffer);
      map = getString(buffer);
      folder = getString(buffer);
      game = getString(buffer);
      id = buffer.getShort();
      players = buffer.get();
      maxPlayers = buffer.get();
      bots = buffer.get();
      type = buffer.get();
      environment = buffer.get();
      visibility = buffer.get();
      vac = buffer.get();
      version = getString(buffer);
    }

    private String getString(ByteBuffer buffer)
    {
      String result = "";
      byte next;
      while ((next = buffer.get()) != 0)
        result += (char) next;
      return result;
    }
  }
}
