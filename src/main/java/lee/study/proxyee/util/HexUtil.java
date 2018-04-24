package lee.study.proxyee.util;

import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;

public class HexUtil {

    public static String bytes2hex03(ByteBuf bytes)
    {
        final String HEX = "0123456789abcdef";
        StringBuilder sb = new StringBuilder(bytes.writerIndex() * 2);

        for (int i = 0; i< bytes.writerIndex(); i++)
        {
            byte b = bytes.getByte(i);
            // 取出这个字节的高4位，然后与0x0f与运算，得到一个0-15之间的数据，通过HEX.charAt(0-15)即为16进制数
            sb.append(HEX.charAt((b >> 4) & 0x0f));
            // 取出这个字节的低位，与0x0f与运算，得到一个0-15之间的数据，通过HEX.charAt(0-15)即为16进制数
            sb.append(HEX.charAt(b & 0x0f));
        }
        return bytes.toString(CharsetUtil.UTF_8);
    }

}
