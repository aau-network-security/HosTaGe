package dk.aau.netsec.hostage.ui.fragment;

import android.app.Fragment;

/**
 * @author Alexander Brakowski
 * @created 12.03.14 16:20
 */
public abstract class UpNavigatibleFragment extends Fragment {
	private Class<?> mUpFragment;
	private boolean mIsUpNavigatible = false;

	private boolean mAllowBack = false;

	public Class<?> getUpFragment(){
		return mUpFragment;
	}

	public void setUpFragment(Class<?> upFragment){
		this.mUpFragment = upFragment;
	}

	public boolean isUpNavigatible(){
		return mIsUpNavigatible;
	}

	public void setUpNavigatible(boolean isUpNavigatible){
		this.mIsUpNavigatible = isUpNavigatible;
	}

	public boolean getAllowBack(){
		return mAllowBack;
	}

	public void setAllowBack(boolean allowBack){
		this.mAllowBack = allowBack;
	}
}
