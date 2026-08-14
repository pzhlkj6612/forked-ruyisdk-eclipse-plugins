package org.ruyisdk.venv.views;

import java.beans.PropertyChangeListener;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.ruyisdk.venv.viewmodel.VenvWizardViewModel;

/**
 * Wizard page shown while package data is loaded asynchronously, keeping the UI responsive.
 */
public class WizardLoadingPage extends WizardPage {

    private final VenvWizardViewModel viewModel;

    private Composite container;
    private Label messageLabel;
    private ProgressBar progressBar;
    private Button retryButton;

    private final PropertyChangeListener listener = e -> updateState();

    WizardLoadingPage(VenvWizardViewModel viewModel) {
        super("loadingPage");
        this.viewModel = viewModel;
        setTitle("Loading");
        setDescription("Fetching package data from the Ruyi package index.");
        setPageComplete(false);
    }

    @Override
    public void createControl(Composite parent) {
        createLayouts(parent);
        addControls();
        registerEvents();
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible && !viewModel.isDataLoadStarted()) {
            viewModel.loadAllAsync();
        }
    }

    private void createLayouts(Composite parent) {
        container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(1, false));

        setControl(container);
    }

    private void addControls() {
        messageLabel = new Label(container, SWT.WRAP);
        messageLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        messageLabel.setText("Package data will be loaded in the background.");

        progressBar = new ProgressBar(container, SWT.INDETERMINATE);
        progressBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        retryButton = new Button(container, SWT.PUSH);
        retryButton.setText("Retry");
        retryButton.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
        retryButton.setVisible(false);
        retryButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                setErrorMessage(null);
                retryButton.setVisible(false);
                viewModel.loadAllAsync();
            }
        });
    }

    private void registerEvents() {
        viewModel.addPropertyChangeListener(listener);
        container.addDisposeListener(e -> viewModel.removePropertyChangeListener(listener));
    }

    private void updateState() {
        if (container.isDisposed()) {
            return;
        }

        final var errorMessage = viewModel.getLoadingErrorMessage();
        if (viewModel.isDataLoading()) {
            messageLabel.setText(viewModel.getLoadingMessage());
            retryButton.setVisible(false);
            setErrorMessage(null);
            setPageComplete(false);
        } else if (!errorMessage.isEmpty()) {
            messageLabel.setText("Failed to load package data.");
            retryButton.setVisible(true);
            setErrorMessage(errorMessage);
            setPageComplete(false);
        } else if (viewModel.isDataLoadStarted()) {
            messageLabel.setText("Package data loaded.");
            retryButton.setVisible(false);
            setErrorMessage(null);
            setPageComplete(true);
            advanceIfCurrent();
        }
        container.layout();
    }

    private void advanceIfCurrent() {
        if (getContainer() != null && getContainer().getCurrentPage() == this) {
            getContainer().showPage(getNextPage());
        }
    }
}
