package com.rootsrecipes.view.discover

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rootsrecipes.databinding.FragmentFilterBinding
import com.rootsrecipes.model.OnItemClickListener
import com.rootsrecipes.utils.BaseFragment
import com.rootsrecipes.utils.Constants
import com.rootsrecipes.utils.Extension
import com.rootsrecipes.utils.Status
import com.rootsrecipes.utils.gone
import com.rootsrecipes.utils.makeToast
import com.rootsrecipes.utils.visible
import com.rootsrecipes.view.discover.adapter.FilterCategoryAdapter
import com.rootsrecipes.view.discover.adapter.FilterCatoDetailsAdapter
import com.rootsrecipes.view.discover.model.FilterCategory
import com.rootsrecipes.view.recipeRecording.viewmodel.RecipeCreateViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel


class FilterFragment : BaseFragment(), OnItemClickListener {
    private var filterTypeFrom: Int = 0
    private lateinit var binding: FragmentFilterBinding
    private lateinit var filterCategoryAdapter: FilterCategoryAdapter
    private lateinit var filterCatoDetailsAdapter: FilterCatoDetailsAdapter
    private var bundle: Bundle? = null
    private val vm: RecipeCreateViewModel by viewModel()
    private var users = ArrayList<String>()

    private lateinit var filterCategoryList: ArrayList<FilterCategory>
    private lateinit var filterCuisineList: ArrayList<FilterCategory>
    private var filterPeopleList = ArrayList<FilterCategory>()

    private var page = 1
    private var limit = 10

    private var isLoading = false
    private var hasMoreData = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getCuisineList()
    }

    @SuppressLint("FragmentLiveDataObserve")
    private fun getCuisineList() {
        binding.pbLoader.visible()
        CoroutineScope(Dispatchers.Main).launch {
            vm.getCuisinesData()
            vm.cuisinesData.observe(this@FilterFragment) { its ->
                when (its.status) {
                    Status.SUCCESS -> {
                        Extension.stopProgress()
                        filterCuisineList =
                            its.data?.data!!.cuisines.map { FilterCategory(it) } as ArrayList<FilterCategory>
                        filterCategoryList = its.data.data.categories.map {
                            FilterCategory(it.name)
                        } as ArrayList<FilterCategory>
                        hitApiToGetPeople()

                    }

                    Status.ERROR -> {
                        Extension.stopProgress()
                        binding.pbLoader.gone()
                        its.message?.let { requireActivity().makeToast(it) }
                    }

                    Status.LOADING -> {

                    }
                }

            }
        }
    }

    private fun setArgumentData() {
        if (arguments != null) {
            bundle = arguments
            val category = bundle!!.getStringArrayList("filteredCategory")
            val cuisine = bundle!!.getStringArrayList("filteredCuisine")
            val people =
                bundle!!.getSerializable("filteredPeople") as? ArrayList<Pair<String, String>>
            filterTypeFrom = bundle!!.getInt(Constants.filterTypeForm)
            //1-Recipe List 2-My Recipes
            if (!category.isNullOrEmpty()) {
                for (i in category) {
                    filterCategoryList.map {
                        if (it.filterName == i) {
                            it.isChecked = true
                        }
                    }
                }
            }
            if (!cuisine.isNullOrEmpty()) {
                for (i in cuisine) {
                    filterCuisineList.map {
                        if (it.filterName == i) {
                            it.isChecked = true
                        }
                    }
                }
            }
            if (filterTypeFrom == 1) {
                if (!people.isNullOrEmpty()) {
                    for (personPair in people) {
                        filterPeopleList.map {
                            if (it.userInfo.first == personPair.first) {
                                it.isChecked = true
                            }
                        }
                    }
                }
            }
            initUi()
            updateTotalFilterChecked()
        }
    }

    @SuppressLint("FragmentLiveDataObserve")
    private fun hitApiForMyCountRecipe() {
        val filteredCategory =
            filterCategoryList.filter { it.isChecked }.map { it.filterName } as ArrayList
        val filteredCuisine =
            filterCuisineList.filter { it.isChecked }.map { it.filterName } as ArrayList
        //   Extension.showProgress(requireActivity())
        if (!(filteredCuisine.isEmpty() && filteredCategory.isEmpty())) {
            CoroutineScope(Dispatchers.Main).launch {
                vm.myCountRecipes(filteredCategory, filteredCuisine)
                vm.countRecipeData.observe(this@FilterFragment) { its ->
                    when (its.status) {
                        Status.SUCCESS -> {
                            Extension.stopProgress()
                            binding.tvTotalValue.text = "${its.data?.data?.count}"
                        }

                        Status.ERROR -> {
                            Extension.stopProgress()
                            its.message?.let { requireActivity().makeToast(it) }
                        }

                        Status.LOADING -> {

                        }
                    }

                }
            }
        }
    }


    @SuppressLint("NotifyDataSetChanged")
    private fun initUi() {
        setAdapterFilterCategory()
        setAdapterFilterDetailsCategory(0)

        binding.apply {
            ivBackFilterRecipes.setOnClickListener {
                val bundle = Bundle()
                val filteredCategory =
                    filterCategoryList.filter { it.isChecked }.map { it.filterName }
                bundle.putStringArrayList("filteredCategory", filteredCategory as ArrayList)
                val filteredCuisine =
                    filterCuisineList.filter { it.isChecked }.map { it.filterName }
                bundle.putStringArrayList("filteredCuisine", filteredCuisine as ArrayList)
                val filteredPeople = filterPeopleList.filter { it.isChecked }.map { it.userInfo }
                bundle.putSerializable(
                    "filteredPeople",
                    ArrayList(filteredPeople)
                ) // Use Serializable
                if (filterCategoryList.none { it.isChecked } && filterCuisineList.none { it.isChecked }) {
                    findNavController().previousBackStackEntry?.savedStateHandle?.set(
                        "requestKey",
                        bundle
                    )
                }
                findNavController().navigateUp()
            }

            btnApplyFilter.setOnClickListener {
                val bundle = Bundle()
                val filteredCategory =
                    filterCategoryList.filter { it.isChecked }.map { it.filterName }
                bundle.putStringArrayList("filteredCategory", filteredCategory as ArrayList)
                val filteredCuisine =
                    filterCuisineList.filter { it.isChecked }.map { it.filterName }
                bundle.putStringArrayList("filteredCuisine", filteredCuisine as ArrayList)
                val filteredPeople = filterPeopleList.filter { it.isChecked }.map { it.userInfo }
                bundle.putSerializable(
                    "filteredPeople",
                    ArrayList(filteredPeople)
                ) // Use Serializable
                findNavController().previousBackStackEntry?.savedStateHandle?.set(
                    "requestKey",
                    bundle
                )
                findNavController().navigateUp()
            }
            tvClear.setOnClickListener {
                filterCuisineList.map { it.isChecked = false }
                filterCategoryList.map { it.isChecked = false }
                filterPeopleList.map { it.isChecked = false }

                filterCatoDetailsAdapter.mList.map { it.isChecked = false }
                filterCatoDetailsAdapter.notifyDataSetChanged()
                updateTotalFilterChecked()
            }
        }
    }

    private fun setAdapterFilterCategory() {
        val filterCategoryList = arrayListOf(
            FilterCategory("Category"),
            FilterCategory("Cuisine"),

            )
        if (filterTypeFrom == 1) {
            filterCategoryList.add(FilterCategory("People"))
        }
        filterCategoryAdapter =
            FilterCategoryAdapter(requireActivity(), filterCategoryList, onItemClickListener = this)
        binding.rvCategoryList.adapter = filterCategoryAdapter
    }

    private fun setAdapterFilterDetailsCategory(filterType: Int) {
        when (filterType) {
            0 -> {
                filterCatoDetailsAdapter =
                    FilterCatoDetailsAdapter(requireActivity(), filterCategoryList, 0, this)
                binding.rvCategoryListDetails.adapter = filterCatoDetailsAdapter
            }

            1 -> {
                filterCatoDetailsAdapter =
                    FilterCatoDetailsAdapter(requireActivity(), filterCuisineList, 0, this)
                binding.rvCategoryListDetails.adapter = filterCatoDetailsAdapter
            }

            2 -> {
                filterCatoDetailsAdapter = FilterCatoDetailsAdapter(
                    requireActivity(),
                    filterPeopleList,
                    1,
                    this@FilterFragment
                )
                binding.rvCategoryListDetails.adapter = filterCatoDetailsAdapter
            }
        }
    }


    override fun onItemClick(position: Int, type: String) {
        when (type) {
            "filter_type" -> {
                setAdapterFilterDetailsCategory(position)
            }

            "filter_details" -> {
                updateTotalFilterChecked()
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateTotalFilterChecked() {
        filterCategoryAdapter.mList[0].totalFilter =
            filterCategoryList.filter { it.isChecked }.size
        filterCategoryAdapter.mList[1].totalFilter =
            filterCuisineList.filter { it.isChecked }.size

        if (filterTypeFrom == 1) {
            filterCategoryAdapter.mList[2].totalFilter =
                filterPeopleList.filter { it.isChecked }.size
        }
        filterCategoryAdapter.notifyDataSetChanged()

        if (filterCategoryList.none { it.isChecked } && filterCuisineList.none { it.isChecked } && filterPeopleList.none { it.isChecked }) {
            binding.clApply.gone()
        } else {
            binding.clApply.visible()
        }
        when (filterTypeFrom) {
            1 -> {
                hitApiForCountRecipe()
            }

            2 -> {
                hitApiForMyCountRecipe()
            }

            0 -> {
                hitApiForMySaveRecipeCount()
            }
        }
    }

    @SuppressLint("FragmentLiveDataObserve")
    private fun hitApiForMySaveRecipeCount() {
        val filteredCategory =
            filterCategoryList.filter { it.isChecked }.map { it.filterName } as ArrayList
        val filteredCuisine =
            filterCuisineList.filter { it.isChecked }.map { it.filterName } as ArrayList
        if (!(filteredCuisine.isEmpty() && filteredCategory.isEmpty())) {
            CoroutineScope(Dispatchers.Main).launch {
                vm.countSaveRecipes(
                    Extension.arrayListToCommaSeparatedString(filteredCategory),
                    Extension.arrayListToCommaSeparatedString(filteredCuisine)
                )
                vm.countSavedRecipeData.observe(this@FilterFragment) { its ->
                    when (its.status) {
                        Status.SUCCESS -> {
                            Extension.stopProgress()
                            binding.tvTotalValue.text = "${its.data?.data?.count}"
                        }

                        Status.ERROR -> {
                            Extension.stopProgress()
                            its.message?.let { requireActivity().makeToast(it) }
                        }

                        Status.LOADING -> {

                        }
                    }

                }
            }
        }
    }

    @SuppressLint("FragmentLiveDataObserve")
    private fun hitApiForCountRecipe() {
        val filteredCategory =
            filterCategoryList.filter { it.isChecked }.map { it.filterName } as ArrayList
        val filteredCuisine =
            filterCuisineList.filter { it.isChecked }.map { it.filterName } as ArrayList
        val filterPeople =
            filterPeopleList.filter { it.isChecked }.map { it.userInfo.first } as ArrayList
        //   Extension.showProgress(requireActivity())

        if (!(filteredCuisine.isEmpty() && filteredCategory.isEmpty() && filterPeople.isEmpty())) {
            CoroutineScope(Dispatchers.Main).launch {
                vm.countPublicRecipes(
                    Extension.arrayListToCommaSeparatedString(filterPeople),
                    Extension.arrayListToCommaSeparatedString(filteredCategory),
                    Extension.arrayListToCommaSeparatedString(filteredCuisine)
                )
                vm.countPublicRecipeData.observe(this@FilterFragment) { its ->
                    when (its.status) {
                        Status.SUCCESS -> {
                            Extension.stopProgress()
                            binding.tvTotalValue.text = "${its.data?.data?.count}"
                        }

                        Status.ERROR -> {
                            Extension.stopProgress()
                            its.message?.let { requireActivity().makeToast(it) }
                        }

                        Status.LOADING -> {

                        }
                    }

                }
            }
        }
    }

    @SuppressLint("FragmentLiveDataObserve")
    private fun hitApiToGetPeople() {
        CoroutineScope(Dispatchers.Main).launch {
            vm.getPeopleFilters(page, limit)
            vm.peopleFilterData.observe(viewLifecycleOwner) { it -> // Use viewLifecycleOwner
                when (it.status) {
                    Status.SUCCESS -> {
                        binding.pbLoader.gone()

                        Extension.stopProgress()
                        filterPeopleList.clear()
                        filterPeopleList.clear()
                        filterPeopleList.clear()
                        it.data?.data?.forEach { person ->
                            filterPeopleList.add(
                                FilterCategory(
                                    filterName = "${person.firstName} ${person.lastName}",
                                    isChecked = false,
                                    totalFilter = it.data.data.size,
                                    profileImage = person.profileImage ?: "",
                                    userInfo = Pair(
                                        person.user_id,
                                        "${person.firstName} ${person.lastName}"
                                    )
                                )
                            )
                        }

// Initialize adapter after data is ready
                        setArgumentData()

                    }

                    Status.ERROR -> {
                        binding.pbLoader.gone()
                        Extension.stopProgress()
                        it.message?.let { requireActivity().makeToast(it) }
                    }

                    Status.LOADING -> {
                    }
                }
            }
        }
    }

    private fun loadMoreItems() {
        isLoading = true
        page++
    }

    private fun setupPagination() {
        binding.rvCategoryListDetails.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                if (!isLoading && hasMoreData) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                        && firstVisibleItemPosition >= 0
                    ) {
                        loadMoreItems()
                    }
                }
            }
        })
    }

}