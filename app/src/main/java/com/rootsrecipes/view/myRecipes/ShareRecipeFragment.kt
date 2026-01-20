package com.rootsrecipes.view.myRecipes

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.rootsrecipes.databinding.FragmentShareRecipeBinding
import com.rootsrecipes.model.OnClickListener
import com.rootsrecipes.utils.BaseFragment
import com.rootsrecipes.utils.Extension
import com.rootsrecipes.utils.Status
import com.rootsrecipes.utils.makeToast
import com.rootsrecipes.view.myRecipes.adapter.ShareAdapter
import com.rootsrecipes.view.myRecipes.model.AllTypeConnection
import com.rootsrecipes.view.myRecipes.viewModel.MyRecipesVM
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel


class ShareRecipeFragment : BaseFragment(), OnClickListener {
    private var recipe_id: String? = null
    private lateinit var shareAdapter: ShareAdapter
    private lateinit var binding: FragmentShareRecipeBinding
    private val myRecipesVM: MyRecipesVM by viewModel()
    private var pageNumber = 1
    private var limit = 10
    private var isLoading = false
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentShareRecipeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUi()
    }

    @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
    private fun initUi() {
        recipe_id = arguments?.getString("recipe_id")
        getAllTypeConnection()
        binding.apply {
            ivBackShareRecipes.setOnClickListener {
                findNavController().navigateUp()
            }
            tvSelectAll.setOnClickListener {
                if (::shareAdapter.isInitialized) {
                    val checkedSelected =
                        shareAdapter.getAllConnection().filter { it.isCheckedUser == true }

                    if (checkedSelected.isNotEmpty()) {
                        shareAdapter.getAllConnection().map { it.isCheckedUser = false }
                    } else {
                        shareAdapter.getAllConnection().map { it.isCheckedUser = true }
                    }
                    binding.tvSelectedItem.text =
                        "Selected: ${shareAdapter.getAllConnection().size}"
                    shareAdapter.notifyDataSetChanged()
                }
            }
            btnProceed.setOnClickListener {
                if (::shareAdapter.isInitialized) {
                    val checkedSelected =
                        shareAdapter.getAllConnection().filter { it.isCheckedUser == true }
                    if (checkedSelected.isNotEmpty()) {
                        hitApiForShareRecipes(checkedSelected)
                    }

                }
            }
            ivCrossBottomTab.setOnClickListener {
                if (::shareAdapter.isInitialized) {
                    shareAdapter.getAllConnection().map { it.isCheckedUser = false }
                    binding.tvSelectedItem.text = "Selected: 0"
                    shareAdapter.notifyDataSetChanged()
                }
            }
        }

    }

    @SuppressLint("FragmentLiveDataObserve")
    private fun hitApiForShareRecipes(checkedSelected: List<AllTypeConnection>) {
        Extension.showProgress(requireActivity())
        val commaSeparatedUserIds =
            checkedSelected.filter { it.isCheckedUser == true }.joinToString(",") { it.user_id }

        CoroutineScope(Dispatchers.Main).launch {
            myRecipesVM.getUserNotify(commaSeparatedUserIds, recipe_id!!)
            myRecipesVM.getUserNotifyData.observe(this@ShareRecipeFragment) {
                when (it.status) {
                    Status.SUCCESS -> {
                        Extension.stopProgress()
                        findNavController().navigateUp()
                    }

                    Status.ERROR -> {
                        Extension.stopProgress()
                    }

                    Status.LOADING -> {

                    }
                }
            }
        }
    }

    @SuppressLint("FragmentLiveDataObserve", "NotifyDataSetChanged")
    private fun getAllTypeConnection() {
        CoroutineScope(Dispatchers.Main).launch {
            Extension.showProgress(requireActivity())
            myRecipesVM.getAllTypeConnection(pageNumber.toString(), limit.toString())
            myRecipesVM.getAllTypeConnectionData.observe(this@ShareRecipeFragment) {
                when (it.status) {
                    Status.SUCCESS -> {
                        Extension.stopProgress()
                        if (pageNumber == 1) {
                            isLoading = false
                            it.data?.let { it1 -> setAdapter(it1.data) }
                        } else {
                            if (::shareAdapter.isInitialized) {
                                isLoading = false
                                it.data?.let { it1 ->
                                    shareAdapter.getAllConnection().addAll(it1.data)
                                }
                                shareAdapter.notifyDataSetChanged()
                            } else {
                                isLoading = false
                                it.data?.let { it1 -> setAdapter(it1.data) }
                            }
                        }
                    }

                    Status.LOADING -> {

                    }

                    Status.ERROR -> {
                        Extension.stopProgress()
                        requireActivity().makeToast(it.message ?: "Something went wrong")
                    }
                }
            }
        }
    }

    private fun setAdapter(data: ArrayList<AllTypeConnection>) {
        shareAdapter = ShareAdapter(requireActivity(), data, this)
        binding.rvShareRecipes.adapter = shareAdapter
        binding.rvShareRecipes.setOnScrollChangeListener { v, _, scrollY, _, _ ->
            val recyclerView = v as RecyclerView
            val lastChild = recyclerView.getChildAt(recyclerView.childCount - 1)
            if (lastChild != null) {
                val diff = lastChild.bottom - (recyclerView.height + scrollY)

                if (!isLoading && diff <= 0) {
                    loadMoreTransactions()
                }
            }
        }
    }

    private fun loadMoreTransactions() {
        isLoading = true
        pageNumber++
        Handler(Looper.getMainLooper()).postDelayed({
            getAllTypeConnection()
        }, 1000)
    }

    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    override fun onClickItem(position: Int) {
        shareAdapter.getAllConnection()[position].isCheckedUser =
            shareAdapter.getAllConnection()[position].isCheckedUser != true

        val filteredCheck = shareAdapter.getAllConnection().filter { it.isCheckedUser == true }
        binding.tvSelectedItem.text = "Selected: " + filteredCheck.size.toString()
        shareAdapter.notifyDataSetChanged()
    }


}