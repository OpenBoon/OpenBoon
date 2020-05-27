import { useState } from 'react'

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
