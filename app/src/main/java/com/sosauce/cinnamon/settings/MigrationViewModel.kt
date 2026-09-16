package com.sosauce.cinnamon.settings

import android.app.Application
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.features.contacts.data.repository.ContactsRepository
import com.sosauce.cinnamon.features.contacts.domain.ContactAddress
import com.sosauce.cinnamon.features.contacts.domain.ContactEmail
import com.sosauce.cinnamon.features.contacts.domain.ContactEvent
import com.sosauce.cinnamon.features.contacts.domain.ContactPhone
import com.sosauce.cinnamon.features.contacts.domain.CuteContact
import com.sosauce.cinnamon.features.contacts.domain.CuteContactDetails
import ezvcard.Ezvcard
import ezvcard.parameter.AddressType
import ezvcard.parameter.EmailType
import ezvcard.parameter.TelephoneType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MigrationViewModel(
    private val application: Application,
    private val contactsRepository: ContactsRepository
) : AndroidViewModel(application) {


    fun handleMigrationAction(action: com.sosauce.cinnamon.settings.MigrationAction) {
        when (action) {
            is MigrationAction.ImportContacts -> {

                Toast.makeText(
                    application,
                    application.getString(R.string.importing_contact),
                    Toast.LENGTH_SHORT
                ).show()

                viewModelScope.launch(Dispatchers.IO) {
                    application.contentResolver.openInputStream(action.vCard)?.use { stream ->
                        val card = Ezvcard.parse(stream).all()

                        card.fastForEach { ezCard ->
                            val firstName = ezCard.structuredName?.given ?: ""
                            val middleName =
                                ezCard.structuredName?.additionalNames?.firstOrNull() ?: ""
                            val lastName = ezCard.structuredName?.family ?: ""
                            val nickname = ezCard.nickname?.values?.firstOrNull() ?: ""
                            val organization = ezCard.organization?.values?.firstOrNull() ?: ""
                            val jobPosition = ezCard.titles.firstOrNull()?.value ?: ""

                            val phoneNumbers = ezCard.telephoneNumbers.mapIndexed { index, phone ->
                                ContactPhone(
                                    number = phone.text ?: "",
                                    type = phone.types.firstOrNull()?.let { mapPhoneType(it) }
                                        ?: ContactsContract.CommonDataKinds.Phone.TYPE_OTHER,
                                    isDefault = index == 0
                                )
                            }

                            val emails = ezCard.emails.mapIndexed { index, email ->
                                ContactEmail(
                                    email = email.value ?: "",
                                    type = email.types.firstOrNull()?.let { mapEmailType(it) }
                                        ?: ContactsContract.CommonDataKinds.Email.TYPE_OTHER,
                                    isDefault = index == 0
                                )
                            }

                            val addresses = ezCard.addresses.mapIndexed { index, address ->
                                ContactAddress(
                                    address = listOfNotNull(
                                        address.streetAddress,
                                        address.locality,
                                        address.region,
                                        address.postalCode,
                                        address.country
                                    ).joinToString(", "),
                                    type = address.types.firstOrNull()?.let { mapAddressType(it) }
                                        ?: ContactsContract.CommonDataKinds.StructuredPostal.TYPE_OTHER,
                                    isDefault = index == 0
                                )
                            }

                            val websites = ezCard.urls.map { it.value ?: "" }
                            val notes = ezCard.notes.firstOrNull()?.value ?: ""

                            val events = ezCard.birthdays.map {
                                ContactEvent(
                                    date = it.date?.toString() ?: "",
                                    type = ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY
                                )
                            } + ezCard.anniversaries.map {
                                ContactEvent(
                                    date = it.date?.toString() ?: "",
                                    type = ContactsContract.CommonDataKinds.Event.TYPE_ANNIVERSARY
                                )
                            }

                            val details = CuteContactDetails(
                                firstName = firstName,
                                middleName = middleName,
                                lastName = lastName,
                                emails = emails,
                                addresses = addresses,
                                websites = websites,
                                note = notes,
                                events = events
                            )

                            val (accountType, accountName) = action.source
                            val cuteContact = CuteContact(
                                phoneNumbers = phoneNumbers,
                                accountType = accountType,
                                accountName = accountName
                            )
                            val success = contactsRepository.createOrEditContact(cuteContact, details)
                            if (success) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        application,
                                        application.getString(R.string.importing_contact_success),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    }
                }
            }

            is MigrationAction.ImportCallLogs -> {

            }
        }
    }

    private fun mapPhoneType(type: TelephoneType): Int = when (type) {
        TelephoneType.HOME -> ContactsContract.CommonDataKinds.Phone.TYPE_HOME
        TelephoneType.WORK -> ContactsContract.CommonDataKinds.Phone.TYPE_WORK
        TelephoneType.CELL -> ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
        TelephoneType.FAX -> ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME
        TelephoneType.PAGER -> ContactsContract.CommonDataKinds.Phone.TYPE_PAGER
        else -> ContactsContract.CommonDataKinds.Phone.TYPE_OTHER
    }

    private fun mapEmailType(type: EmailType): Int = when (type) {
        EmailType.HOME -> ContactsContract.CommonDataKinds.Email.TYPE_HOME
        EmailType.WORK -> ContactsContract.CommonDataKinds.Email.TYPE_WORK
        else -> ContactsContract.CommonDataKinds.Email.TYPE_OTHER
    }

    private fun mapAddressType(type: AddressType): Int = when (type) {
        AddressType.HOME -> ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME
        AddressType.WORK -> ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK
        else -> ContactsContract.CommonDataKinds.StructuredPostal.TYPE_OTHER
    }

}

sealed interface MigrationAction {
    data class ImportContacts(
        val source: Pair<String, String>,
        val vCard: Uri
    ) : MigrationAction

    data class ImportCallLogs(
        val json: Uri
    ) : MigrationAction
}
