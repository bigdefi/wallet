package org.BigDefi.accounts

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.core.content.ContextCompat.getColor
import androidx.core.widget.ImageViewCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.LEFT
import androidx.recyclerview.widget.ItemTouchHelper.RIGHT
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_list_addresses.*
import kotlinx.android.synthetic.main.item_address_book.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kethereum.keystore.api.KeyStore
import org.koin.android.ext.android.inject
import org.ligi.kaxt.setVisibility
import org.ligi.kaxt.startActivityFromClass
import org.BigDefi.R
import org.BigDefi.base_activities.startAddressReceivingActivity
import org.BigDefi.data.addresses.AddressBookEntry
import org.BigDefi.data.addresses.CurrentAddressProvider
import org.BigDefi.data.addresses.faucet
import org.BigDefi.data.addresses.getSpec
import org.BigDefi.enhancedlist.BaseEnhancedListActivity
import org.BigDefi.enhancedlist.EnhancedListAdapter
import org.BigDefi.enhancedlist.EnhancedListInterface
import org.BigDefi.util.copyToClipboard

abstract class BaseAddressBookActivity : BaseEnhancedListActivity<AddressBookEntry>() {

    override val enhancedList by lazy {
        object : EnhancedListInterface<AddressBookEntry> {
            override suspend fun undeleteAll() = appDatabase.addressBook.unDeleteAll()
            override suspend fun getAll() = appDatabase.addressBook.all()
            override fun compare(t1: AddressBookEntry, t2: AddressBookEntry) = t1.address == t2.address
            override suspend fun upsert(item: AddressBookEntry) = appDatabase.addressBook.upsert(item)
            override suspend fun deleteAllSoftDeleted() {
                lifecycleScope.launch(Dispatchers.Default) {
                    appDatabase.addressBook.run {
                        allDeleted().forEach {
                            keyStore.deleteKey(it.address)
                        }
                        deleteAllSoftDeleted()
                    }
                }
            }

            override fun filter(item: AddressBookEntry) = (!settings.filterAddressesStared || item.starred)
                    && (!settings.filterAddressesKeyOnly || keyStore.hasKeyForForAddress(item.address))
                    && checkForSearchTerm(item.name, item.note ?: "", item.address.hex)
                    && item != faucet

        }
    }


    val keyStore: KeyStore by inject()
    val currentAddressProvider: CurrentAddressProvider by inject()

    abstract fun onAddressClick(addressEntry: AddressBookEntry)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        supportActionBar?.subtitle = getString(R.string.address_book_subtitle)

        fab.setOnClickListener {
            startActivityFromClass(CreateAccountActivity::class.java)
        }

        val simpleItemTouchCallback = object : ItemTouchHelper.SimpleCallback(0, LEFT or RIGHT) {

            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, swipeDir: Int) {

                val current = adapter.displayList[viewHolder.adapterPosition]

                current.changeDeleteState(true)

            }
        }

        val itemTouchHelper = ItemTouchHelper(simpleItemTouchCallback)
        itemTouchHelper.attachToRecyclerView(recycler_view)
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_address_list, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.menu_stared_only).isChecked = settings.filterAddressesStared
        menu.findItem(R.id.menu_only_with_key).isChecked = settings.filterAddressesKeyOnly
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {

        R.id.menu_stared_only -> item.filterToggle {
            settings.filterAddressesStared = it
        }
        R.id.menu_only_with_key -> item.filterToggle {
            settings.filterAddressesKeyOnly = it
        }

        else -> super.onOptionsItemSelected(item)
    }


    override val adapter: EnhancedListAdapter<AddressBookEntry> by lazy {
        EnhancedListAdapter<AddressBookEntry>(
                layout = R.layout.item_address_book,
                bind = { entry, view ->
                    val spec = entry.getSpec()


                    view.setOnClickListener {
                        onAddressClick(entry)
                    }

                    view.account_delete_button.setOnClickListener {
                        view.deleteWithAnimation(entry)
                    }

                    view.account_copy_button.setOnClickListener {
                        copyToClipboard(entry.address, fab)
                    }

                    view.address_name.text = entry.name

                    ACCOUNT_TYPE_MAP[spec?.type]?.drawable.let {
                        view.key_indicator.setImageResource(it ?: R.drawable.ic_watch)
                    }
                    val hasKeyForForAddress = keyStore.hasKeyForForAddress(entry.address)
                    view.key_indicator_source.setVisibility(spec?.source?.isNotBlank() == true)
                    if (hasKeyForForAddress) {
                        view.key_indicator.setOnClickListener {
                            startAddressReceivingActivity(entry.address, ExportKeyActivity::class.java)
                        }
                        ImageViewCompat.setImageTintList(view.key_indicator, ColorStateList.valueOf(getColor(this, R.color.colorAccent)))
                    } else {
                        ImageViewCompat.setImageTintList(view.key_indicator, ColorStateList.valueOf(getColor(this, R.color.fgColor)))
                    }


                    if (entry.note == null || entry.note!!.isBlank()) {
                        view.address_note.visibility = GONE
                    } else {
                        view.address_note.visibility = VISIBLE
                        view.address_note.text = entry.note
                    }

                    view.address_hash.text = entry.address.hex

                    view.edit_account.setOnClickListener {
                        startAddressReceivingActivity(entry.address, EditAccountActivity::class.java)
                    }

                    view.address_starred.setImageResource(
                            if (entry.starred) {
                                R.drawable.ic_star_24dp
                            } else {
                                R.drawable.ic_star_border_24dp
                            }
                    )

                    view.address_starred.setOnClickListener {
                        lifecycleScope.launch {
                            val updatedEntry = entry.copy(starred = !entry.starred)
                            appDatabase.addressBook.upsert(updatedEntry)
                            refreshAdapter()
                        }
                    }

                }
        )


    }
}
