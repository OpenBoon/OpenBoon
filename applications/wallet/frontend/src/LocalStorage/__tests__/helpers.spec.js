import TestRenderer, { act } from 'react-test-renderer'

import { useLocalStorage } from '../helpers'

describe('<LocalStorage /> helpers', () => {
  const { getItem, setItem, clear } = localStorage

  const mockSet = jest.fn()
  const mockGet = jest.fn()
  const mockClear = jest.fn()

  Object.defineProperty(window, 'localStorage', {
    value: { setItem: mockSet, getItem: mockGet, clear: mockClear },
  })

  afterAll(() => {
    Object.defineProperty(window, 'localStorage', {
      value: { setItem, getItem, clear },
    })
  })

  describe('useLocalStorage()', () => {
    it('should return properly', () => {
      const TestComponent = () => {
        const [value, setValue] = useLocalStorage({
          key: 'name',
          initialState: 100,
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
        const [value, setValue] = useLocalStorage({ key: 'name' })
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
        const [value, setValue] = useLocalStorage({
          key: 'name',
          initialState: 'initialState',
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
      ).toEqual('initialState')
    })
  })

  describe('useLocalStorage() with reducer', () => {
    it('should handle a corrupt value', () => {
      mockGet.mockImplementationOnce(() => 'invalid json')

      const TestComponent = () => {
        const [state, dispatch] = useLocalStorage({
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
