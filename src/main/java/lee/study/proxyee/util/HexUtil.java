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
            sb.append(HEX.charAt((b >> 4) & 0x0f));
            sb.append(HEX.charAt(b & 0x0f));
        }
        return bytes.toString(CharsetUtil.UTF_8);
    }

}
