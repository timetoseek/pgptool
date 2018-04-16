package org.pgptool.gui.hintsforusage.impl;

import java.util.LinkedList;
import java.util.List;

import org.pgptool.gui.hintsforusage.api.HintsCoordinator;
import org.pgptool.gui.hintsforusage.api.HintsHolder;
import org.pgptool.gui.hintsforusage.ui.HintHost;
import org.pgptool.gui.hintsforusage.ui.HintPm;
import org.springframework.beans.factory.annotation.Autowired;
import org.summerb.approaches.jdbccrud.api.dto.EntityChangedEvent;

import com.google.common.base.Preconditions;
import com.google.common.eventbus.EventBus;

/**
 * This class will basically show hints one after another. It wont know any
 * details regarding particular hints, it will just queue them when
 * {@link #scheduledHints}
 * 
 * @author sergeyk
 *
 */
public class HintsCoordinatorImpl implements HintsCoordinator {
	private HintsHolder hintsHolder;
	private List<HintPm> scheduledHints = new LinkedList<>();

	@Autowired
	private EventBus eventBus;

	@Override
	synchronized public void scheduleHint(HintPm hintPm) {
		Preconditions.checkArgument(hintPm != null);
		if (scheduledHints.contains(hintPm) || (hintsHolder != null && hintPm.equals(hintsHolder.getHint()))) {
			return;
		}

		hintPm.setHintHost(hintHost);
		eventBus.post(EntityChangedEvent.added(hintPm));

		if (hintsHolder != null && hintsHolder.getHint() == null) {
			hintsHolder.setHint(hintPm);
		} else {
			scheduledHints.add(hintPm);
		}
	}

	@Override
	synchronized public void cancelHint(HintPm hintPm) {
		Preconditions.checkArgument(hintPm != null);

		if (hintsHolder != null && hintPm.equals(hintsHolder.getHint())) {
			nextHint();
		} else if (scheduledHints.contains(hintPm)) {
			boolean removed = scheduledHints.remove(hintPm);
			if (removed) {
				eventBus.post(EntityChangedEvent.removedObject(hintPm));
			}
		}
	}

	@Override
	public boolean isHintScheduled(Class<? extends HintPm> hintClazz) {
		if (hintsHolder != null && hintsHolder.getHint() != null
				&& hintClazz.equals(hintsHolder.getHint().getClass())) {
			return true;
		}
		return scheduledHints.stream().map(x -> hintClazz.equals(x.getClass())).filter(x -> x == true).count() > 0;
	}

	private HintHost hintHost = new HintHost() {
		@Override
		public void onClose() {
			nextHint();
		}
	};

	private void nextHint() {
		if (hintsHolder == null) {
			return;
		}

		HintPm current = hintsHolder.getHint();
		hintsHolder.setHint(scheduledHints.size() == 0 ? null : scheduledHints.remove(0));

		if (current != null) {
			eventBus.post(EntityChangedEvent.removedObject(current));
		}
	}

	public HintsHolder getHintsHolder() {
		return hintsHolder;
	}

	@Override
	public void setHintsHolder(HintsHolder hintsHolder) {
		this.hintsHolder = hintsHolder;
		if (hintsHolder != null) {
			nextHint();
		}
	}

}
