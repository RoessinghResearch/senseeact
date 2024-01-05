// The ElementAnimator supports click handlers that animate an element while
// executing an asynchronous function. It waits until both the animation and the
// asynchronous function are completed before executing the callback.
// During this time any other clicks are ignored to avoid parallel execution.
// But to prevent an irresponsive interface in case an event does not occur,
// there is a maximum time during which other clicks are blocked. This is set
// in the following constant in milliseconds.
// Example use case:
// User clicks on a reply button in a dialogue. An asynchronous API call is
// executed while the button is animated. If during the execution, the user
// clicks again (on another button, or a double click on the same button), no
// new API call should be made, since that would lead to unexpected results.
const ELEMENT_ANIMATOR_CLICK_HANDLER_WAIT = 2000;

class ElementAnimator {
	/**
	 * Member variables:
	 * - _currentClick: object with these properties:
	 *     - id: UUID
	 *     - time: moment object with the click time
	 *     - animationCompleted: boolean
	 *     - asyncHandlerCompleted: boolean
	 *     - asyncHandlerResult: result of async handler, set when handler is
	 *         completed
	 *     - callback: callback function to call when animnationCompleted and
	 *         asyncHandlerCompleted. The value of asyncHandlerResult is passed
	 *         as a parameter.
	 */

	/**
	 * Adds an animated click handler to an element. When the element is
	 * clicked, it will add the specified animation class and start the
	 * specified asynchronous handler. The handler is called with one parameter:
	 * a click ID. When the handler is completed, it should call
	 * onAnimatedClickHandlerCompleted() with the click ID and the result.
	 * When both the animation and the handler are completed, the specified
	 * callback will be called with the result from the handler.
	 *
	 * You may set any parameter except "clickElem" and "animElem" to null. For
	 * example if you don't want to call an asynchronous function, but you want
	 * the callback to be called after just an animation, you can set
	 * asyncHandler to null.
	 *
	 * While a click animation or asynchronous handler is running, all other
	 * clicks are ignored. But this only happens with a maximum duration of
	 * ELEMENT_ANIMATOR_CLICK_HANDLER_WAIT. See its declaration for more info.
	 *
	 * - clickElem: the element that receives the click
	 * - animElem: the element that should be animated
	 * - animClass: the animation class or null
	 * - asyncHandler: the asynchronous handler function or null. The function
	 *     receives one parameter: a click ID. Eventually the handler must
	 *     call onAnimatedClickHandlerCompleted().
	 * - callback: the callback function or null. The function receives one
	 *     parameter: the result of the asynchronous handler (set with
	 *     onAnimatedClickHandlerCompleted())
	 */
	addAnimatedClickHandler(clickElem, animElem, animClass, asyncHandler,
			callback) {
		var self = this;
		clickElem.on('click', function(event) {
			event.preventDefault();
			self._onAnimatedClick(animElem, animClass, asyncHandler, callback);
		});
	}
	
	clearAnimatedClickHandler(clickElem) {
		clickElem.off('click');
	}

	/**
	 * This method should be called by the asyncHandler function that is passed
	 * to addAnimatedClickHandler(). It should always be called when the handler
	 * is completed (successfully or with an error).
	 *
	 * - clickId: the click ID that was passed to the asyncHandler function
	 * - result: the result that will be passed to the callback function
	 *     specified in addAnimatedClickHandler()
	 */
	onAnimatedClickHandlerCompleted(clickId, result) {
		if (!this._currentClick || this._currentClick.id != clickId)
			return;
		this._currentClick.asyncHandlerCompleted = true
		this._currentClick.asyncHandlerResult = result;
		this._checkAnimatedClickCompleted();
	}

	/**
	 * Called when the user clicked. Starts the animation and the asynchronous
	 * handler.
	 *
	 * - elem: jQuery element that should be animated
	 * - animClass: the animation class or null
	 * - asyncHandler: the asynchronous handler function or null. The function
	 *     receives one parameter: a click ID. Eventually the handler must
	 *     call onAnimatedClickHandlerCompleted().
	 * - callback: the callback function or null. The function receives one
	 *     parameter: the result of the asynchronous handler (set with
	 *     onAnimatedClickHandlerCompleted())
	 */
	_onAnimatedClick(elem, animClass, asyncHandler, callback) {
		let time = moment();
		if (this._currentClick) {
			let endTime = moment(this._currentClick.time);
			endTime.add(ELEMENT_ANIMATOR_CLICK_HANDLER_WAIT, 'ms');
			if (time.isBefore(endTime))
				return;
		}
		var clickId = uuidv4();
		this._currentClick = {
			id: clickId,
			time: time,
			animationCompleted: animClass == null,
			asyncHandlerCompleted: asyncHandler == null,
			asyncHandlerResult: null,
			callback: callback
		};
		var self = this;
		elem.off('animationend');
		if (animClass) {
			elem.on('animationend', function(ev) {
				ev.stopPropagation();
				self._onAnimatedClickAnimationCompleted(clickId, elem,
					animClass);
			});
			elem.addClass(animClass);
		}
		if (asyncHandler)
			asyncHandler(clickId);
		this._checkAnimatedClickCompleted();
	}

	_onAnimatedClickAnimationCompleted(clickId, elem, animClass) {
		elem.removeClass(animClass);
		if (!this._currentClick || this._currentClick.id != clickId)
			return;
		this._currentClick.animationCompleted = true;
		this._checkAnimatedClickCompleted();
	}

	_checkAnimatedClickCompleted() {
		if (!this._currentClick.animationCompleted ||
				!this._currentClick.asyncHandlerCompleted) {
			return;
		}
		if (this._currentClick.callback)
			this._currentClick.callback(this._currentClick.asyncHandlerResult);
		this._currentClick = null;
	}

	startAnimation(elem, animClass, onend = null) {
		elem.off('animationend');
		var self = this;
		elem.on('animationend', function(ev) {
			ev.stopPropagation();
			self._onAnimationEnd(elem, animClass, onend);
		});
		elem.addClass(animClass);
	}

	_onAnimationEnd(elem, animClass, onend) {
		elem.removeClass(animClass);
		if (onend)
			onend();
	}
}
