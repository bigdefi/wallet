package org.BigDefi.data

import java.math.BigInteger

val ETH_IN_WEI = BigInteger("1000000000000000000")

var DEFAULT_GAS_PRICE = BigInteger("20000000000")
var DEFAULT_GAS_LIMIT_ETH_TX = BigInteger("21000")
var DEFAULT_GAS_LIMIT_ERC_20_TX = BigInteger("73000")

const val DEFAULT_PASSWORD = "default"

const val DEFAULT_ETHEREUM_BIP44_PATH = "m/44'/60'/0'/0/0"

const val KEY_TX_HASH = "TXHASH"


const val REQUEST_CODE_CREATE_ACCOUNT = 420
const val REQUEST_CODE_PICK_ACCOUNT_TYPE = REQUEST_CODE_CREATE_ACCOUNT + 1
const val REQUEST_CODE_SELECT_TOKEN = REQUEST_CODE_PICK_ACCOUNT_TYPE + 1
const val REQUEST_CODE_ENTER_NFC_CREDENTIALS = REQUEST_CODE_SELECT_TOKEN + 1

const val REQUEST_CODE_PICK_WATCH_ONLY = REQUEST_CODE_ENTER_NFC_CREDENTIALS + 1
const val REQUEST_CODE_PICK_NFC = REQUEST_CODE_PICK_WATCH_ONLY + 1
const val REQUEST_CODE_IMPORT = REQUEST_CODE_PICK_NFC + 1
const val REQUEST_CODE_ENTER_PIN = REQUEST_CODE_IMPORT + 1
const val REQUEST_CODE_ENTER_PASSWORD = REQUEST_CODE_ENTER_PIN + 1
const val REQUEST_CODE_IMPORT_AS = REQUEST_CODE_ENTER_PASSWORD + 1
const val REQUEST_CODE_SCAN_QR = REQUEST_CODE_IMPORT_AS + 1
const val REQUEST_CODE_OPEN_DOCUMENT = REQUEST_CODE_SCAN_QR + 1
const val REQUEST_CODE_CREATE_DOCUMENT = REQUEST_CODE_OPEN_DOCUMENT + 1
const val REQUEST_CODE_NFC = REQUEST_CODE_CREATE_DOCUMENT + 1
const val REQUEST_CODE_CREATE_TX = REQUEST_CODE_NFC + 1
const val REQUEST_CODE_SIGN_TX = REQUEST_CODE_CREATE_TX + 1
const val REQUEST_CODE_SELECT_TO_ADDRESS = REQUEST_CODE_SIGN_TX + 1
const val REQUEST_CODE_SELECT_FROM_ADDRESS = REQUEST_CODE_SELECT_TO_ADDRESS +1

const val EXTRA_KEY_ADDRESS = "address"
const val EXTRA_KEY_NFC_CREDENTIALS = "nfc_credentials"
const val EXTRA_KEY_ACCOUNTSPEC = "accountspec"
const val EXTRA_KEY_PIN = "pin"
const val EXTRA_KEY_PWD = "pwd"

const val ACCOUNT_TYPE_NONE = "none"
const val ACCOUNT_TYPE_BURNER = "burner"
const val ACCOUNT_TYPE_PIN_PROTECTED = "pinprotected"
const val ACCOUNT_TYPE_PASSWORD_PROTECTED = "pwdprotected"
const val ACCOUNT_TYPE_TREZOR = "trezor"
const val ACCOUNT_TYPE_IMPORT = "import"
const val ACCOUNT_TYPE_NFC = "nfc"
const val ACCOUNT_TYPE_WATCH_ONLY = "watchonly"
const val ACCOUNT_TYPE_KEEPKEY = "keepkey"
