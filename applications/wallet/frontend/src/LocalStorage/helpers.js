import { useState, useReducer, useEffect } from 'react'

export const useLocalStorageState = ({ key, initialValue }) => {
  const [storedValue, setStoredValue] = useState(() => {
    const item = localStorage.getItem(key)

    try {
      return item ? JSON.parse(item) : initialValue
    } catch (error) {
      return initialValue
    }
  })

  const setValue = ({ value }) => {
    setStoredValue(value)
    localStorage.setItem(key, JSON.stringify(value))
  }

  return [storedValue, setValue]
}

export const useLocalStorageReducer = ({ key, reducer, initialState }) => {
  const [state, dispatch] = useReducer(reducer, initialState, () => {
    const item = localStorage.getItem(key)

    try {
      return item ? JSON.parse(item) : initialState
    } catch (error) {
      return initialState
    }
  })

  useEffect(() => {
    localStorage.setItem(key, JSON.stringify(state))
  }, [key, state])

  return [state, dispatch]
}
