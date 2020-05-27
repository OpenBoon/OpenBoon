import TestRenderer, { act } from 'react-test-renderer'

import { useLocalStorageState, useLocalStorageReducer } from '../helpers'

describe('<LocalStorage /> helpers', () => {
  const mockSet = jest.fn()
  const mockGet = jest.fn()

  Object.defineProperty(window, 'localStorage', {
    value: { setItem: mockSet, getItem: mockGet },
  })

  describe('useLocalStorageState()', () => {
    it('should return properly', () => {
      const TestComponent = () => {
        const [value, setValue] = useLocalStorageState({
          key: 'name',
          initialValue: 100,
        })
        return (
          <button type="button" onClick={setValue}>
            {value}
          </button>
        )
      }

      const component = TestRenderer.create(<TestComponent />)

      act(() => {
        component.root
          .findByProps({ type: 'button' })
          .props.onClick({ value: 200 })
      })

      expect(mockGet).toHaveBeenCalledWith('name')
      expect(mockSet).toHaveBeenCalledWith('name', '200')
    })

    it('should get existing values', () => {
      mockGet.mockImplementationOnce(() => 200)

      const TestComponent = () => {
        const [value, setValue] = useLocalStorageState({ key: 'name' })
        return (
          <button type="button" onClick={setValue}>
            {value}
          </button>
        )
      }

      const component = TestRenderer.create(<TestComponent />)

      expect(
        component.root.findByProps({ type: 'button' }).props.children,
      ).toEqual(200)
    })

    it('should handle a corrupt value', () => {
      mockGet.mockImplementationOnce(() => 'invalid json')

      const TestComponent = () => {
        const [value, setValue] = useLocalStorageState({
          key: 'name',
          initialValue: 'initialValue',
        })
        return (
          <button type="button" onClick={setValue}>
            {value}
          </button>
        )
      }

      const component = TestRenderer.create(<TestComponent />)

      expect(
        component.root.findByProps({ type: 'button' }).props.children,
      ).toEqual('initialValue')
    })
  })

  describe('useLocalStorageReducer()', () => {
    it('should handle a corrupt value', () => {
      mockGet.mockImplementationOnce(() => 'invalid json')

      const TestComponent = () => {
        const [state, dispatch] = useLocalStorageReducer({
          key: 'name',
          reducer: () => {},
          initialState: 'initialState',
        })
        return (
          <button type="button" onClick={() => dispatch()}>
            {state}
          </button>
        )
      }

      const component = TestRenderer.create(<TestComponent />)

      expect(
        component.root.findByProps({ type: 'button' }).props.children,
      ).toEqual('initialState')
    })
  })
})
