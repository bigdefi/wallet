package org.BigDefi.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import org.BigDefi.data.addresses.AddressBookDAO;
import org.BigDefi.data.addresses.AddressBookEntry;
import org.BigDefi.data.balances.Balance;
import org.BigDefi.data.balances.BalanceDAO;
import org.BigDefi.data.chaininfo.ChainInfo;
import org.BigDefi.data.chaininfo.ChainInfoDAO;
import org.BigDefi.data.tokens.Token;
import org.BigDefi.data.tokens.TokenDAO;
import org.BigDefi.data.transactions.TransactionDAO;
import org.BigDefi.data.transactions.TransactionEntity;

@Database(entities = {AddressBookEntry.class, Balance.class, ChainInfo.class, Token.class, TransactionEntity.class}, version = 6)
@TypeConverters({RoomTypeConverters.class})
public abstract class AppDatabase extends RoomDatabase {

    public abstract AddressBookDAO getAddressBook();

    public abstract TokenDAO getTokens();

    public abstract TransactionDAO getTransactions();

    public abstract BalanceDAO getBalances();

    public abstract ChainInfoDAO getChainInfo();

}
