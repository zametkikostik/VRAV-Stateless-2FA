package com.example

import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testWalletGenerationAndLoading() {
    val (privKey, address) = EthereumCryptoUtils.generateWallet()
    assertNotNull(privKey)
    assertNotNull(address)
    assertTrue(privKey.startsWith("0x"))
    assertTrue(address.startsWith("0x"))

    val loadedAddress = EthereumCryptoUtils.loadWallet(privKey)
    assertEquals(address.lowercase(), loadedAddress.lowercase())
  }

  @Test
  fun testPersonalSignAndVerification() {
    val (privKey, address) = EthereumCryptoUtils.generateWallet()
    val message = "Test signing message"
    val signature = EthereumCryptoUtils.personalSign(message, privKey)
    assertNotNull(signature)
    assertTrue(signature.startsWith("0x"))

    val isValid = EthereumCryptoUtils.verifyPersonalSignature(message, signature, address)
    assertTrue("Verification failed", isValid)
  }
}
