package com.nhphong.mockcreator.sample

interface CreateBBMojiContract {
	interface View {
		fun syncBBMojiStatus()
		fun openGrantPage(appProperty: AppProperty)
		fun displayLoading(enable: Boolean)
		fun displayError(error: String, recoverable: Boolean = false)
	}

	abstract class Presenter: BasePresenter<View>() {
		abstract fun login()
		abstract fun loginWithAuthCode(authCode: String?)
		abstract fun onBackFromGrantPage(authCode: String?)
	}
}
