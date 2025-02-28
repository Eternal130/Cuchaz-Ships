/*******************************************************************************
 * Copyright (c) 2014 jeff.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     jeff - initial API and implementation
 ******************************************************************************/
package cuchaz.ships.packets;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.network.NetHandlerPlayClient;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cuchaz.modsShared.net.Packet;
import cuchaz.ships.config.BlockProperties;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class PacketBlockPropertiesOverrides extends Packet<PacketBlockPropertiesOverrides> {
	
	private String m_overrides;
	
	public PacketBlockPropertiesOverrides() {
		// for registration
	}
	
	public PacketBlockPropertiesOverrides(String overrides) {
		m_overrides = overrides;
	}

	private static byte[] compress(byte[] input) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try (GZIPOutputStream gzip = new GZIPOutputStream(bos)) {
			gzip.write(input);
		}
		return bos.toByteArray();
	}

	private static byte[] decompress(byte[] input) throws IOException {
		ByteArrayInputStream bis = new ByteArrayInputStream(input);
		try (GZIPInputStream gzip = new GZIPInputStream(bis);
			 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			byte[] buffer = new byte[1024];
			int len;
			while ((len = gzip.read(buffer)) > 0) {
				out.write(buffer, 0, len);
			}
			return out.toByteArray();
		}
	}
	@Override
	public void toBytes(ByteBuf buf) {
		byte[] rawBytes = m_overrides.getBytes(StandardCharsets.UTF_8);
        byte[] compressed = null;
        try {
            compressed = compress(rawBytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        buf.writeInt(compressed.length);
		buf.writeBytes(compressed);
	}
	
	@Override
	public void fromBytes(ByteBuf buf) {
		int length = buf.readInt();
		byte[] compressed = new byte[length];
		buf.readBytes(compressed);
        byte[] rawBytes = null;
        try {
            rawBytes = decompress(compressed);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        m_overrides = new String(rawBytes, StandardCharsets.UTF_8);
	}
	
	// boilerplate code is annoying...
	@Override
	public IMessageHandler<PacketBlockPropertiesOverrides,IMessage> getClientHandler() {
		return new IMessageHandler<PacketBlockPropertiesOverrides,IMessage>() {
			
			@Override
			public IMessage onMessage(PacketBlockPropertiesOverrides message, MessageContext ctx) {
				return message.onReceivedClient(ctx.getClientHandler());
			}
		};
	}
	
	@SideOnly(Side.CLIENT)
	protected IMessage onReceivedClient(NetHandlerPlayClient netClient) {
		// received on the client
		// save the block properties overrides
		BlockProperties.setOverrides(m_overrides);
		
		return null;
	}
}
