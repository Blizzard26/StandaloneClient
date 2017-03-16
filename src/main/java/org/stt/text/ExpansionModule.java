package org.stt.text;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

public class ExpansionModule extends AbstractModule {

	@Override
	protected void configure() {
    	Multibinder<ExpansionProvider> expansionProviderBinder = Multibinder.newSetBinder(binder(), ExpansionProvider.class);
    	expansionProviderBinder.addBinding().to(SplitItemGrouper.class);
    	expansionProviderBinder.addBinding().to(JiraExpansionProvider.class);
	}

}
