import { useRef, useEffect } from 'react'

export const noop = () => () => {}

const scrollers = {}

export const getScroller = ({ namespace }) => {
  if (!scrollers[namespace]) {
    scrollers[namespace] = {}
  }

  const register = ({ eventName, callback }) => {
    if (!scrollers[namespace][eventName]) {
      scrollers[namespace][eventName] = []
    }

    scrollers[namespace][eventName].push(callback)

    const deregister = () => {
      const callbacks = scrollers[namespace][eventName]
      const index = callbacks.indexOf(callback)

      /* istanbul ignore else */
      if (index > -1) {
        callbacks.splice(index, 1)
      }
    }

    return deregister
  }

  const emit = ({ eventName, data }) => {
    const callbacks = scrollers[namespace][eventName] || []

    callbacks.forEach((callback) => {
      callback(data)
    })
  }

  const scroller = { register, emit }

  return scroller
}

export const getOnEvent = ({ node, scroller }) => {
  return (event) => {
    event.preventDefault()

    const scrollX =
      event.type === 'wheel' ? event.deltaX : event.target.scrollLeft
    const scrollY =
      event.type === 'wheel' ? event.deltaY : event.target.scrollTop

    scroller.emit({ eventName: event.type, data: { node, scrollX, scrollY } })
  }
}

export const handleEventListener = ({ eventName, node, scroller }) => {
  const onEvent = getOnEvent({ node, scroller })

  node.addEventListener(eventName, onEvent)

  return () => node.removeEventListener(eventName, onEvent)
}

export const computeScroll = ({ eventName, scrollX, scrollY, node }) => {
  let newScrollLeft
  let newScrollTop

  if (eventName === 'wheel') {
    const maxScrollX = node.scrollWidth - node.clientWidth
    const newScrollX = node.scrollLeft + scrollX
    newScrollLeft = Math.max(0, Math.min(newScrollX, maxScrollX))

    const maxScrollY = node.scrollHeight - node.clientHeight
    const newScrollY = node.scrollTop + scrollY
    newScrollTop = Math.max(0, Math.min(newScrollY, maxScrollY))
  }

  if (eventName === 'scroll') {
    newScrollLeft = scrollX
    newScrollTop = scrollY
  }

  // eslint-disable-next-line no-param-reassign
  node.scrollLeft = newScrollLeft
  // eslint-disable-next-line no-param-reassign
  node.scrollTop = newScrollTop
}

export const getCallback = ({ eventName, node }) => {
  return ({ scrollX, scrollY }) =>
    computeScroll({ eventName, scrollX, scrollY, node })
}

export const initializeScroller = ({
  namespace,
  nodeRef,
  isWheelEmitter,
  isWheelListener,
  isScrollEmitter,
  isScrollListener,
}) => {
  const scroller = getScroller({ namespace })

  const deregisterCallbacks = []

  const node = nodeRef.current

  if (!node) return noop

  /* istanbul ignore else */
  if (isWheelEmitter) {
    const deregister = handleEventListener({
      eventName: 'wheel',
      node,
      scroller,
    })
    deregisterCallbacks.push(deregister)
  }

  /* istanbul ignore else */
  if (isScrollEmitter) {
    const deregister = handleEventListener({
      eventName: 'scroll',
      node,
      scroller,
    })
    deregisterCallbacks.push(deregister)
  }

  /* istanbul ignore else */
  if (isWheelListener) {
    const wheelScrollCallback = getCallback({ eventName: 'wheel', node })

    const deregister = scroller.register({
      eventName: 'wheel',
      callback: wheelScrollCallback,
    })

    deregisterCallbacks.push(deregister)
  }

  /* istanbul ignore else */
  if (isScrollListener) {
    const innerScrollCallback = getCallback({ eventName: 'scroll', node })

    const deregister = scroller.register({
      eventName: 'scroll',
      callback: innerScrollCallback,
    })

    deregisterCallbacks.push(deregister)
  }

  return () => {
    deregisterCallbacks.forEach((callback) => callback())
  }
}

export const useScroller = ({
  namespace,
  isWheelEmitter,
  isWheelListener,
  isScrollEmitter,
  isScrollListener,
}) => {
  const nodeRef = useRef()

  useEffect(
    () =>
      initializeScroller({
        namespace,
        nodeRef,
        isWheelEmitter,
        isWheelListener,
        isScrollEmitter,
        isScrollListener,
      }),
    [
      namespace,
      nodeRef,
      isWheelEmitter,
      isWheelListener,
      isScrollEmitter,
      isScrollListener,
    ],
  )

  return nodeRef
}
