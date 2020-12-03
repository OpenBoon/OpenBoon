import { reducer, ACTIONS } from '../reducer'

const INITIAL_STATE = {
  size: 400,
  originSize: 400,
  isOpen: true,
}

describe('<Resizeable /> reducer', () => {
  it('should handle mouse move', () => {
    expect(
      reducer(INITIAL_STATE, {
        type: ACTIONS.HANDLE_MOUSE_MOVE,
        payload: { minSize: 400, newSize: 500 },
      }),
    ).toEqual({ ...INITIAL_STATE, size: 500, originSize: 400, isOpen: true })
  })

  it('should handle mouse up above min size', () => {
    expect(
      reducer(INITIAL_STATE, {
        type: ACTIONS.HANDLE_MOUSE_UP,
        payload: { minSize: 400, newSize: 500 },
      }),
    ).toEqual({ ...INITIAL_STATE, size: 500, originSize: 400, isOpen: true })
  })

  it('should handle mouse up below min size from open', () => {
    expect(
      reducer(INITIAL_STATE, {
        type: ACTIONS.HANDLE_MOUSE_UP,
        payload: { minSize: 400, newSize: 300 },
      }),
    ).toEqual({ ...INITIAL_STATE, size: 400, originSize: 0, isOpen: false })
  })

  it('should handle mouse up below min size from closed', () => {
    expect(
      reducer(
        { ...INITIAL_STATE, isOpen: false },
        {
          type: ACTIONS.HANDLE_MOUSE_UP,
          payload: { minSize: 400, newSize: 300 },
        },
      ),
    ).toEqual({ ...INITIAL_STATE, size: 400, originSize: 400, isOpen: true })
  })

  it('should handle open', () => {
    expect(
      reducer(
        { ...INITIAL_STATE, isOpen: false },
        {
          type: ACTIONS.OPEN,
          payload: { minSize: 400, newSize: 300 },
        },
      ),
    ).toEqual({ ...INITIAL_STATE, originSize: 400, isOpen: true })
  })

  it('should handle close', () => {
    expect(
      reducer(
        { ...INITIAL_STATE, isOpen: true },
        {
          type: ACTIONS.CLOSE,
          payload: {},
        },
      ),
    ).toEqual({ ...INITIAL_STATE, originSize: 0, isOpen: false })
  })

  it('should handle update', () => {
    expect(
      reducer(
        { ...INITIAL_STATE, extra: 'initialProp' },
        {
          type: ACTIONS.UPDATE,
          payload: { extra: 'newProp' },
        },
      ),
    ).toEqual({ ...INITIAL_STATE, extra: 'newProp' })
  })

  it('should default', () => {
    expect(reducer({ size: 400, originSize: 300, isOpen: true }, '')).toEqual({
      size: 400,
      originSize: 300,
      isOpen: true,
    })
  })
})
