package com.rootsrecipes.view.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.rootsrecipes.databinding.FragmentShareAppBinding
import com.rootsrecipes.model.OnItemClickListener
import com.rootsrecipes.utils.BaseFragment
import com.rootsrecipes.view.setting.adapter.ShareAppAdapter
import com.rootsrecipes.view.setting.adapter.ShareContact


class ShareAppFragment : BaseFragment(), OnItemClickListener {

    private lateinit var shareAppAdapter: ShareAppAdapter
    private lateinit var binding: FragmentShareAppBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentShareAppBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUi()
    }

    private fun initUi() {
        loadContacts()
        setOnClickMethod()
        setupRecyclerView()
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

    private fun loadContacts() {
        // Sample data - replace with your data source
        val contacts = arrayListOf(
            ShareContact("1", "Brooklyn Simmons", "url1", "@brooksim123"),
            ShareContact("2", "Albert Flores", "url2", "@albertf"),
            ShareContact("3", "Theresa Webb", "url3", "@theresa"),
            ShareContact("4", "John Doe", "url4", "@johndoe"),
            ShareContact("5", "Jane Smith", "url5", "@janesmith"),
            ShareContact("6", "Michael Johnson", "url6", "@michaelj"),
            ShareContact("7", "Emily Davis", "url7", "@emilyd"),
            ShareContact("8", "Daniel Brown", "url8", "@danielb"),
            ShareContact("9", "Sophia Wilson", "url9", "@sophiaw"),
            ShareContact("10", "James Anderson", "url10", "@jamesa"),
            ShareContact("11", "Olivia Martinez", "url11", "@oliviam"),
            ShareContact("12", "Benjamin Taylor", "url12", "@benjamint"),
            ShareContact("13", "Emma Thomas", "url13", "@emmat"),
            ShareContact("14", "Lucas Garcia", "url14", "@lucasg"),
            ShareContact("15", "Mia Rodriguez", "url15", "@miar"),
            ShareContact("16", "Alexander Lee", "url16", "@alexlee"),
            ShareContact("17", "Amelia Walker", "url17", "@ameliaw"),
            ShareContact("18", "Ethan Hall", "url18", "@ethanh"),
            ShareContact("19", "Ava Young", "url19", "@avayoung"),
            ShareContact("20", "Jacob Allen", "url20", "@jacoba"),
            ShareContact("21", "Charlotte King", "url21", "@charlottek"),
            ShareContact("22", "William Wright", "url22", "@willwright"),
            ShareContact("23", "Scarlett Scott", "url23", "@scarletts"),
            ShareContact("24", "Henry Turner", "url24", "@henryt"),
            ShareContact("25", "Ella Moore", "url25", "@ellamoore"),
            ShareContact("26", "Sebastian Green", "url26", "@sebgreen"),
            ShareContact("27", "Luna Baker", "url27", "@lunabaker"),
            ShareContact("28", "Samuel Perez", "url28", "@samperez"),
            ShareContact("29", "Grace Adams", "url29", "@gracea"),
            ShareContact("30", "David Carter", "url30", "@davidc"),
            ShareContact("31", "Chloe Mitchell", "url31", "@chloem"),
            ShareContact("32", "Logan Phillips", "url32", "@loganp"),
            ShareContact("33", "Layla Evans", "url33", "@laylae"),
            ShareContact("34", "Matthew Collins", "url34", "@matthewc"),
            ShareContact("35", "Zoe Stewart", "url35", "@zoes"),
            ShareContact("36", "Jack Morris", "url36", "@jackm"),
            ShareContact("37", "Hannah Bailey", "url37", "@hannahb"),
            ShareContact("38", "Leo Rivera", "url38", "@leor"),
            ShareContact("39", "Victoria Hughes", "url39", "@victoriah"),
            ShareContact("40", "Ryan Clark", "url40", "@ryanc"),
            ShareContact("41", "Aubrey Ward", "url41", "@aubreyw"),
            ShareContact("42", "Nathan Ramirez", "url42", "@nathanr"),
            ShareContact("43", "Penelope Brooks", "url43", "@penbrooks"),
            ShareContact("44", "Carter Morgan", "url44", "@carterm"),
            ShareContact("45", "Aria Sanders", "url45", "@arias"),
            ShareContact("46", "Owen Cooper", "url46", "@owenc"),
            ShareContact("47", "Isabella Reed", "url47", "@isabellar"),
            ShareContact("48", "Elijah Rogers", "url48", "@elijahr"),
            ShareContact("49", "Stella Gray", "url49", "@stellag"),
            ShareContact("50", "Liam Peterson", "url50", "@liamp"),
            ShareContact("51", "Natalie Cox", "url51", "@nataliec"),
            ShareContact("52", "Noah Foster", "url52", "@noahf"),
            ShareContact("53", "Mila Howard", "url53", "@milah"),
            ShareContact("54", "Mason Jenkins", "url54", "@masonj"),
            ShareContact("55", "Leah Patterson", "url55", "@leahp"),
            ShareContact("56", "Dylan Bryant", "url56", "@dylanb"),
            ShareContact("57", "Sophie Torres", "url57", "@sophiet"),
            ShareContact("58", "Gabriel Simmons", "url58", "@gabriels"),
            ShareContact("59", "Paisley Butler", "url59", "@paisleyb"),
            ShareContact("60", "Eli Wood", "url60", "@eliwood"),
            ShareContact("61", "Ellie Barnes", "url61", "@ellieb"),
            ShareContact("62", "Julian Coleman", "url62", "@julianc"),
            ShareContact("63", "Lillian Jenkins", "url63", "@lillianj"),
            ShareContact("64", "Caleb Hernandez", "url64", "@calebh"),
            ShareContact("65", "Violet Fisher", "url65", "@violetf"),
            ShareContact("66", "Isaac Daniels", "url66", "@isaacd"),
            ShareContact("67", "Hazel Hunter", "url67", "@hazelh"),
            ShareContact("68", "Andrew Hayes", "url68", "@andrewh"),
            ShareContact("69", "Aurora Murphy", "url69", "@auroram"),
            ShareContact("70", "Josiah Bell", "url70", "@josiahb"),
            ShareContact("71", "Savannah Powell", "url71", "@savannahp"),
            ShareContact("72", "Charles Scott", "url72", "@charless"),
            ShareContact("73", "Addison Myers", "url73", "@addisonm"),
            ShareContact("74", "Thomas Russell", "url74", "@thomasr"),
            ShareContact("75", "Naomi Ross", "url75", "@naomir"),
            ShareContact("76", "Christopher Hughes", "url76", "@chrish"),
            ShareContact("77", "Lucy Diaz", "url77", "@lucyd"),
            ShareContact("78", "Evan Kelly", "url78", "@evank"),
            ShareContact("79", "Bella Price", "url79", "@bellap"),
            ShareContact("80", "Jonathan Sanders", "url80", "@jonathans"),
            ShareContact("81", "Claire Richardson", "url81", "@clairer"),
            ShareContact("82", "Hunter Campbell", "url82", "@hunterc"),
            ShareContact("83", "Willow Reed", "url83", "@willowr"),
            ShareContact("84", "Anthony Watson", "url84", "@anthonyw"),
            ShareContact("85", "Lila Rivera", "url85", "@lilar"),
            ShareContact("86", "Adrian Richardson", "url86", "@adrianr"),
            ShareContact("87", "Piper Gomez", "url87", "@piperg"),
            ShareContact("88", "Aaron Patterson", "url88", "@aaronp"),
            ShareContact("89", "Ruby Bennett", "url89", "@rubyb"),
            ShareContact("90", "Hudson Jenkins", "url90", "@hudsonj"),
            ShareContact("91", "Madison Sanders", "url91", "@madisons"),
            ShareContact("92", "Asher Campbell", "url92", "@asherc"),
            ShareContact("93", "Emily Torres", "url93", "@emilyt"),
            ShareContact("94", "Elias Wood", "url94", "@eliasw"),
            ShareContact("95", "Elena Morris", "url95", "@elenam"),
            ShareContact("96", "Dominic Cooper", "url96", "@dominicc"),
            ShareContact("97", "Lydia Parker", "url97", "@lydiap"),
            ShareContact("98", "Connor Young", "url98", "@connory"),
            ShareContact("99", "Maeve Mitchell", "url99", "@maevem"),
            ShareContact("100", "Nathaniel Allen", "url100", "@nathaniela")
        )
        shareAppAdapter = ShareAppAdapter(requireContext(), contacts, this)
        binding.rvPersons.adapter = shareAppAdapter
        setupAlphabetScroller()
    }

    private fun setupAlphabetScroller() {
        binding.alphabetScrollBar.setOnLetterSelectedListener { letter ->
            shareAppAdapter.scrollToSection(letter, binding.rvPersons)
        }
    }

    override fun onItemClick(position: Int, type: String) {

    }


}