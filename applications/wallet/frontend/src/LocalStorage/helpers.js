import { useState, useEffect, useRef } from 'react'

const emitters = {}

const registerEmitter = ({ key, callback, initialState }) => {
  if (!emitters[key]) {
    emitters[key] = {
      callbacks: [],
      value: initialState,
    }
  }
  emitters[key].callbacks.push(callback)

  return {
    deregister: () => {
      const { callbacks } = emitters[key]
      const index = callbacks.indexOf(callback)

      /* istanbul ignore else */
      if (index > -1) {
        callbacks.splice(index, 1)
      }
    },
    emit: ({ value }) => {
      if (emitters[key].value !== value) {
        emitters[key].value = value

        emitters[key].callbacks.forEach((c) => {
          if (c !== callback) {
            c(value)
          }
        })
      }
    },
  }
}

export const useLocalStorage = ({ key, initialState, reducer }) => {
  const emitterRef = useRef(null)

  const [state, setState] = useState(() => {
    const item = localStorage.getItem(key)

    try {
      return item ? JSON.parse(item) : initialState
    } catch (error) {
      return initialState
    }
  })

  const dispatch = reducer
    ? (action) => {
        const nextState = reducer(state, action)
        setState(nextState)
        return action
      }
    : ({ value }) => setState(value)

  useEffect(() => {
    emitterRef.current = registerEmitter({
      key,
      callback: setState,
      initialState,
    })

    return () => emitterRef.current.deregister()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    localStorage.setItem(key, JSON.stringify(state))

    emitterRef.current.emit({ value: state })
  }, [key, state])

  return [state, dispatch]
}
