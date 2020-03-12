import { reducer, ACTIONS } from '../reducer'

describe('<Assets /> reducer', () => {
  it('should increment', () => {
    expect(
      reducer(
        { thumbnailCount: 4, isMin: false, isMax: false },
        { type: ACTIONS.INCREMENT },
      ),
    ).toEqual({ thumbnailCount: 1, isMin: false, isMax: true })
  })

  it('should decrement', () => {
    expect(
      reducer(
        { thumbnailCount: 4, isMin: false, isMax: false },
        { type: ACTIONS.DECREMENT },
      ),
    ).toEqual({
      thumbnailCount: 8,
      isMin: true,
      isMax: false,
    })
  })

  it('should default', () => {
    expect(
      reducer({ thumbnailCount: 4, isMin: false, isMax: false }, ''),
    ).toEqual({ thumbnailCount: 4, isMin: false, isMax: false })
  })
})
