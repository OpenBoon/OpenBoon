import {
  noop,
  getScroller,
  getOnEvent,
  handleEventListener,
  computeScroll,
  getCallback,
  initializeScroller,
} from '../helpers'

describe('<Scroll /> helpers', () => {
  describe('noop()', () => {
    it('noop should do nothing', () => {
      expect(noop()()).toBe(undefined)
    })
  })

  describe('getScroller()', () => {
    it('should return properly', () => {
      const mockCallback = jest.fn()
      const mockCallBackAgain = jest.fn()

      const scroller = getScroller({ namespace: 'NameSpace' })

      scroller.register({
        eventName: 'eventName',
        callback: mockCallback,
      })

      scroller.register({
        eventName: 'eventName',
        callback: mockCallBackAgain,
      })

      scroller.emit({ eventName: 'nonExistentName' })

      scroller.emit({ eventName: 'eventName' })

      expect(mockCallback).toHaveBeenCalled()
      expect(mockCallBackAgain).toHaveBeenCalled()
    })
  })

  describe('getOnEvent()', () => {
    it('should generate a callback for a wheel event', () => {
      const mockEmit = jest.fn()
      const mockPreventDefault = jest.fn()
      const mockEvent = {
        type: 'wheel',
        deltaX: 100,
        deltaY: 100,
        preventDefault: mockPreventDefault,
      }

      const onEvent = getOnEvent({ scroller: { emit: mockEmit } })

      onEvent(mockEvent)

      expect(mockPreventDefault).toHaveBeenCalled()
      expect(mockEmit).toHaveBeenCalledWith({
        eventName: 'wheel',
        data: { scrollX: 100, scrollY: 100 },
      })
    })

    it('should generate a callback for a scroll event', () => {
      const mockEmit = jest.fn()
      const mockPreventDefault = jest.fn()
      const mockEvent = {
        type: 'scroll',
        target: { scrollLeft: 100, scrollTop: 100 },
        preventDefault: mockPreventDefault,
      }

      const onEvent = getOnEvent({ scroller: { emit: mockEmit } })

      onEvent(mockEvent)

      expect(mockEmit).toHaveBeenCalledWith({
        eventName: 'scroll',
        data: { scrollX: 100, scrollY: 100 },
      })
    })
  })

  describe('handleEventListener()', () => {
    it('should generate callback for inner scroll', () => {
      const mockAddEventListener = jest.fn()
      const mockRemoveEventListener = jest.fn()
      const mockEmit = jest.fn()

      const mockNode = {
        scrollLeft: 0,
        scrollTop: 0,
        addEventListener: mockAddEventListener,
        removeEventListener: mockRemoveEventListener,
      }
      const mockScroller = { emit: mockEmit }

      const deregister = handleEventListener({
        eventName: 'eventName',
        node: mockNode,
        scroller: mockScroller,
      })

      expect(mockAddEventListener).toHaveBeenCalled()

      deregister()

      expect(mockRemoveEventListener).toHaveBeenCalled()
    })
  })

  describe('computeScroll()', () => {
    it('should scroll properly for a wheel event', () => {
      const mockNode = {
        scrollWidth: 100,
        scrollHeight: 100,
        clientWidth: 50,
        clientHeight: 50,
        scrollLeft: 0,
        scrollTop: 0,
      }

      computeScroll({
        eventName: 'wheel',
        scrollX: 100,
        scrollY: 100,
        node: mockNode,
      })

      expect(mockNode).toEqual({
        scrollWidth: 100,
        scrollHeight: 100,
        clientWidth: 50,
        clientHeight: 50,
        scrollLeft: 50,
        scrollTop: 50,
      })
    })

    it('should scroll properly for a scroll event', () => {
      const mockNode = {
        scrollLeft: 0,
        scrollTop: 0,
      }

      computeScroll({
        eventName: 'scroll',
        scrollX: 100,
        scrollY: 100,
        node: mockNode,
      })

      expect(mockNode).toEqual({
        scrollLeft: 100,
        scrollTop: 100,
      })
    })
  })

  describe('getCallback()', () => {
    it('should properly generate a callback', () => {
      const mockNode = {
        scrollWidth: 100,
        scrollHeight: 100,
        clientWidth: 50,
        clientHeight: 50,
        scrollLeft: 0,
        scrollTop: 0,
      }

      const callback = getCallback({ eventName: 'wheel', node: mockNode })

      callback({ scrollX: 100, scrollY: 100 })

      expect(mockNode).toEqual({
        scrollWidth: 100,
        scrollHeight: 100,
        clientWidth: 50,
        clientHeight: 50,
        scrollLeft: 50,
        scrollTop: 50,
      })
    })
  })

  describe('initializeScroller()', () => {
    it('should return properly', () => {
      const mockAddEventListener = jest.fn()
      const mockRemoveEventListener = jest.fn()
      const mockNodeRef = {
        current: {
          addEventListener: mockAddEventListener,
          removeEventListener: mockRemoveEventListener,
        },
      }

      const deregisterCallbacks = initializeScroller({
        namespace: 'TestScroller',
        nodeRef: mockNodeRef,
        isWheelEmitter: true,
        isWheelListener: true,
        isScrollEmitter: true,
        isScrollListener: true,
      })

      expect(mockAddEventListener).toHaveBeenCalledTimes(2)

      deregisterCallbacks()

      expect(mockRemoveEventListener).toHaveBeenCalledTimes(2)
    })

    it('should return properly without a node', () => {
      const deregisterCallbacks = initializeScroller({
        namespace: 'TestScroller',
        nodeRef: {},
        isWheelEmitter: true,
        isWheelListener: true,
        isScrollEmitter: true,
        isScrollListener: true,
      })

      expect(deregisterCallbacks).toEqual(noop)
    })
  })
})
