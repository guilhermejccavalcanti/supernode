/*
 * Copyright 2013 Tamas Blummer tamas@bitsofproof.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bitsofproof.supernode.api;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.Security;
import java.util.Arrays;
import java.util.Random;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.json.JSONArray;
import org.json.JSONException;
import org.junit.BeforeClass;
import org.junit.Test;

import com.bitsofproof.supernode.api.KeyFormatter.PassphraseProvider;

public class KeySerializerTest
{
	@BeforeClass
	public static void init ()
	{
		Security.addProvider (new BouncyCastleProvider ());
	}

	private static final String WIF = "WIF.json";
	private static final String BIP38NoEC = "BIP38NoEC.json";
	private static final String BIP38EC = "BIP38EC.json";

	private JSONArray readObjectArray (String resource) throws IOException, JSONException
	{
		InputStream input = this.getClass ().getResource ("/" + resource).openStream ();
		StringBuffer content = new StringBuffer ();
		byte[] buffer = new byte[1024];
		int len;
		while ( (len = input.read (buffer)) > 0 )
		{
			byte[] s = new byte[len];
			System.arraycopy (buffer, 0, s, 0, len);
			content.append (new String (buffer, "UTF-8"));
		}
		return new JSONArray (content.toString ());
	}

	@Test
	public void wifTest () throws IOException, JSONException, ValidationException
	{
		KeyFormatter formatter = new KeyFormatter (null, chain);
		JSONArray testData = readObjectArray (WIF);
		for ( int i = 0; i < testData.length (); ++i )
		{
			JSONArray test = testData.getJSONArray (i);
			ECKeyPair kp = formatter.parseSerializedKey (test.getString (1));
			String address = AddressConverter.toSatoshiStyle (Hash.keyHash (kp.getPublic ()), false, chain);
			assertTrue (test.getString (0).equals (address));
			String serialized = formatter.serializeKey (kp);
			assertTrue (test.getString (1).equals (serialized));
		}
	}

	@Test
	public void bip38NoECTest () throws ValidationException, IOException, JSONException
	{
		JSONArray testData = readObjectArray (BIP38NoEC);
		for ( int i = 0; i < testData.length (); ++i )
		{
			final JSONArray test = testData.getJSONArray (i);
			final PassphraseProvider pp = new PassphraseProvider ()
			{
				@Override
				public String getPassphrase ()
				{
					try
					{
						return test.getString (2);
					}
					catch ( JSONException e )
					{
						return null;
					}
				}
			};
			KeyFormatter formatter = new KeyFormatter (pp, chain);

			ECKeyPair kp = formatter.parseSerializedKey (test.getString (0));
			String unencrypted = KeyFormatter.serializeWIF (kp);
			assertTrue (test.getString (1).equals (unencrypted));
			ECKeyPair kp2 = formatter.parseSerializedKey (unencrypted);
			assertTrue (formatter.serializeKey (kp2).equals (test.getString (0)));
		}
	}

	@Test
	public void bip38ECTest () throws ValidationException, IOException, JSONException
	{
		JSONArray testData = readObjectArray (BIP38EC);
		for ( int i = 0; i < testData.length (); ++i )
		{
			final JSONArray test = testData.getJSONArray (i);
			final PassphraseProvider pp = new PassphraseProvider ()
			{
				@Override
				public String getPassphrase ()
				{
					try
					{
						return test.getString (2);
					}
					catch ( JSONException e )
					{
						return null;
					}
				}
			};
			KeyFormatter formatter = new KeyFormatter (pp, chain);
			ECKeyPair kp = formatter.parseSerializedKey (test.getString (0));
			String unencrypted = KeyFormatter.serializeWIF (kp);
			assertTrue (test.getString (1).equals (unencrypted));
		}
	}

	@Test
	public void bip38RandomTest () throws ValidationException
	{
		for ( int i = 0; i < 10; ++i )
		{
			final long seed = i;
			PassphraseProvider pp = new PassphraseProvider ()
			{
				@Override
				public String getPassphrase ()
				{
					Random r = new Random (seed);
					StringBuffer p = new StringBuffer ();
					for ( int i = 0; i < 30; ++i )
					{
						p.append ((char) (r.nextInt () % 0x1FFFFF));
					}
					return p.toString ();
				}
			};

			KeyFormatter formatter = new KeyFormatter (pp, chain);
			ECKeyPair kp = ECKeyPair.createNew (i % 2 == 0);
			String serialized = formatter.serializeKey (kp);
			ECKeyPair kp2 = formatter.parseSerializedKey (serialized);
			assertTrue (Arrays.equals (kp.getPublic (), kp2.getPublic ()));
			assertTrue (Arrays.equals (kp.getPrivate (), kp2.getPrivate ()));
		}
	}

	private static ChainParameter chain = new ChainParameter ()
	{

		@Override
		public BigInteger getMinimumTarget ()
		{
			return null;
		}

		@Override
		public long getRewardForHeight (int height)
		{
			return 0;
		}

		@Override
		public int getDifficultyReviewBlocks ()
		{
			return 0;
		}

		@Override
		public int getTargetBlockTime ()
		{
			return 0;
		}

		@Override
		public boolean isProduction ()
		{
			return true;
		}

		@Override
		public int getAddressFlag ()
		{
			return 0;
		}

		@Override
		public int getMultisigAddressFlag ()
		{
			return 5;
		}
	};
}