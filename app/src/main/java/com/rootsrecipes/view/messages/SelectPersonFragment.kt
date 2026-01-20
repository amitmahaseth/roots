package com.rootsrecipes.view.messages

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.rootsrecipes.R
import com.rootsrecipes.databinding.FragmentSelectPersonBinding
import com.rootsrecipes.utils.BaseFragment
import com.rootsrecipes.utils.Constants
import com.rootsrecipes.utils.Status
import com.rootsrecipes.utils.gone
import com.rootsrecipes.utils.makeToast
import com.rootsrecipes.utils.visible
import com.rootsrecipes.view.messages.adapter.Contact
import com.rootsrecipes.view.messages.adapter.PersonsAdapter
import com.rootsrecipes.view.myRecipes.model.AllTypeConnection
import com.rootsrecipes.view.myRecipes.viewModel.MyRecipesVM
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class SelectPersonFragment : BaseFragment() {
    private lateinit var personAdapter: PersonsAdapter
    private lateinit var binding: FragmentSelectPersonBinding
    private var typeFromValue = -1
    private val myRecipesVM: MyRecipesVM by viewModel()
    private var pageNumber = "1"
    private var limit = "10"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSelectPersonBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bundle = this.arguments
        typeFromValue = bundle?.getInt(Constants.typeFrom)!!
        initUi()
    }

    private fun initUi() {
        setupRecyclerView()
        getAllTypeConnection()
        setUi()
//        loadContacts(it.data.data)
        setOnClickMethod()
    }

    private fun setUi() {
        binding.apply {
            if (typeFromValue == 0) {
                tvHeaderSelectPerson.text = getString(R.string.new_message)
                clBottomTab.gone()
            } else if (typeFromValue == 1) {
                tvHeaderSelectPerson.text = getString(R.string.share_with_connection)
                clBottomTab.visible()
            }
        }
    }

    private fun setOnClickMethod() {
        binding.apply {
            ivBackSelectPerson.setOnClickListener { findNavController().popBackStack() }
        }
    }

    private fun setupRecyclerView() {
        binding.rvPersons.apply {
            layoutManager = LinearLayoutManager(requireContext())
        }
    }
    @SuppressLint("FragmentLiveDataObserve")
    private fun getAllTypeConnection() {
        CoroutineScope(Dispatchers.Main).launch {
            myRecipesVM.getAllTypeConnection(pageNumber, limit)
            myRecipesVM.getAllTypeConnectionData.observe(this@SelectPersonFragment) {
                when (it.status) {
                    Status.SUCCESS -> {
                        it.data?.let { it1 -> loadContacts(it1.data) }
                    }

                    Status.LOADING -> {

                    }

                    Status.ERROR -> {
                        requireActivity().makeToast(it.message ?: "Something went wrong")
                    }
                }
            }
        }

    }

    private fun loadContacts(data: ArrayList<AllTypeConnection>) {
        // Sample data - replace with your data source
      /*  val contacts = arrayListOf(
            Contact("1", "Brooklyn Simmons", "url1", "@brooksim123"),
            Contact("2", "Albert Flores", "url2", "@albertf"),
            Contact("3", "Theresa Webb", "url3", "@theresa"),
            Contact("4", "John Doe", "url4", "@johndoe"),
            Contact("5", "Jane Smith", "url5", "@janesmith"),
            Contact("6", "Michael Johnson", "url6", "@michaelj"),
            Contact("7", "Emily Davis", "url7", "@emilyd"),
            Contact("8", "Daniel Brown", "url8", "@danielb"),
            Contact("9", "Sophia Wilson", "url9", "@sophiaw"),
            Contact("10", "James Anderson", "url10", "@jamesa"),
            Contact("11", "Olivia Martinez", "url11", "@oliviam"),
            Contact("12", "Benjamin Taylor", "url12", "@benjamint"),
            Contact("13", "Emma Thomas", "url13", "@emmat"),
            Contact("14", "Lucas Garcia", "url14", "@lucasg"),
            Contact("15", "Mia Rodriguez", "url15", "@miar"),
            Contact("16", "Alexander Lee", "url16", "@alexlee"),
            Contact("17", "Amelia Walker", "url17", "@ameliaw"),
            Contact("18", "Ethan Hall", "url18", "@ethanh"),
            Contact("19", "Ava Young", "url19", "@avayoung"),
            Contact("20", "Jacob Allen", "url20", "@jacoba"),
            Contact("21", "Charlotte King", "url21", "@charlottek"),
            Contact("22", "William Wright", "url22", "@willwright"),
            Contact("23", "Scarlett Scott", "url23", "@scarletts"),
            Contact("24", "Henry Turner", "url24", "@henryt"),
            Contact("25", "Ella Moore", "url25", "@ellamoore"),
            Contact("26", "Sebastian Green", "url26", "@sebgreen"),
            Contact("27", "Luna Baker", "url27", "@lunabaker"),
            Contact("28", "Samuel Perez", "url28", "@samperez"),
            Contact("29", "Grace Adams", "url29", "@gracea"),
            Contact("30", "David Carter", "url30", "@davidc"),
            Contact("31", "Chloe Mitchell", "url31", "@chloem"),
            Contact("32", "Logan Phillips", "url32", "@loganp"),
            Contact("33", "Layla Evans", "url33", "@laylae"),
            Contact("34", "Matthew Collins", "url34", "@matthewc"),
            Contact("35", "Zoe Stewart", "url35", "@zoes"),
            Contact("36", "Jack Morris", "url36", "@jackm"),
            Contact("37", "Hannah Bailey", "url37", "@hannahb"),
            Contact("38", "Leo Rivera", "url38", "@leor"),
            Contact("39", "Victoria Hughes", "url39", "@victoriah"),
            Contact("40", "Ryan Clark", "url40", "@ryanc"),
            Contact("41", "Aubrey Ward", "url41", "@aubreyw"),
            Contact("42", "Nathan Ramirez", "url42", "@nathanr"),
            Contact("43", "Penelope Brooks", "url43", "@penbrooks"),
            Contact("44", "Carter Morgan", "url44", "@carterm"),
            Contact("45", "Aria Sanders", "url45", "@arias"),
            Contact("46", "Owen Cooper", "url46", "@owenc"),
            Contact("47", "Isabella Reed", "url47", "@isabellar"),
            Contact("48", "Elijah Rogers", "url48", "@elijahr"),
            Contact("49", "Stella Gray", "url49", "@stellag"),
            Contact("50", "Liam Peterson", "url50", "@liamp"),
            Contact("51", "Natalie Cox", "url51", "@nataliec"),
            Contact("52", "Noah Foster", "url52", "@noahf"),
            Contact("53", "Mila Howard", "url53", "@milah"),
            Contact("54", "Mason Jenkins", "url54", "@masonj"),
            Contact("55", "Leah Patterson", "url55", "@leahp"),
            Contact("56", "Dylan Bryant", "url56", "@dylanb"),
            Contact("57", "Sophie Torres", "url57", "@sophiet"),
            Contact("58", "Gabriel Simmons", "url58", "@gabriels"),
            Contact("59", "Paisley Butler", "url59", "@paisleyb"),
            Contact("60", "Eli Wood", "url60", "@eliwood"),
            Contact("61", "Ellie Barnes", "url61", "@ellieb"),
            Contact("62", "Julian Coleman", "url62", "@julianc"),
            Contact("63", "Lillian Jenkins", "url63", "@lillianj"),
            Contact("64", "Caleb Hernandez", "url64", "@calebh"),
            Contact("65", "Violet Fisher", "url65", "@violetf"),
            Contact("66", "Isaac Daniels", "url66", "@isaacd"),
            Contact("67", "Hazel Hunter", "url67", "@hazelh"),
            Contact("68", "Andrew Hayes", "url68", "@andrewh"),
            Contact("69", "Aurora Murphy", "url69", "@auroram"),
            Contact("70", "Josiah Bell", "url70", "@josiahb"),
            Contact("71", "Savannah Powell", "url71", "@savannahp"),
            Contact("72", "Charles Scott", "url72", "@charless"),
            Contact("73", "Addison Myers", "url73", "@addisonm"),
            Contact("74", "Thomas Russell", "url74", "@thomasr"),
            Contact("75", "Naomi Ross", "url75", "@naomir"),
            Contact("76", "Christopher Hughes", "url76", "@chrish"),
            Contact("77", "Lucy Diaz", "url77", "@lucyd"),
            Contact("78", "Evan Kelly", "url78", "@evank"),
            Contact("79", "Bella Price", "url79", "@bellap"),
            Contact("80", "Jonathan Sanders", "url80", "@jonathans"),
            Contact("81", "Claire Richardson", "url81", "@clairer"),
            Contact("82", "Hunter Campbell", "url82", "@hunterc"),
            Contact("83", "Willow Reed", "url83", "@willowr"),
            Contact("84", "Anthony Watson", "url84", "@anthonyw"),
            Contact("85", "Lila Rivera", "url85", "@lilar"),
            Contact("86", "Adrian Richardson", "url86", "@adrianr"),
            Contact("87", "Piper Gomez", "url87", "@piperg"),
            Contact("88", "Aaron Patterson", "url88", "@aaronp"),
            Contact("89", "Ruby Bennett", "url89", "@rubyb"),
            Contact("90", "Hudson Jenkins", "url90", "@hudsonj"),
            Contact("91", "Madison Sanders", "url91", "@madisons"),
            Contact("92", "Asher Campbell", "url92", "@asherc"),
            Contact("93", "Emily Torres", "url93", "@emilyt"),
            Contact("94", "Elias Wood", "url94", "@eliasw"),
            Contact("95", "Elena Morris", "url95", "@elenam"),
            Contact("96", "Dominic Cooper", "url96", "@dominicc"),
            Contact("97", "Lydia Parker", "url97", "@lydiap"),
            Contact("98", "Connor Young", "url98", "@connory"),
            Contact("99", "Maeve Mitchell", "url99", "@maevem"),
            Contact("100", "Nathaniel Allen", "url100", "@nathaniela")
        )*/
        personAdapter = PersonsAdapter(requireContext(), data, typeFromValue)
        binding.rvPersons.adapter = personAdapter
        setupAlphabetScroller()
    }

    private fun setupAlphabetScroller() {
        binding.alphabetScrollBar.setOnLetterSelectedListener { letter ->
            personAdapter.scrollToSection(letter, binding.rvPersons)
        }
    }

}