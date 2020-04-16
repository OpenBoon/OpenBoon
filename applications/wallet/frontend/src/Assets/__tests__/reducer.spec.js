import { reducer, ACTIONS } from '../reducer'

describe('<Assets /> reducer', () => {
  it('should increment', () => {
    expect(
      reducer(
        { columnCount: 4, isMin: false, isMax: false },
        { type: ACTIONS.INCREMENT },
      ),
    ).toEqual({ columnCount: 1, isMin: false, isMax: true })
  })

  it('should decrement', () => {
    expect(
      reducer(
        { columnCount: 4, isMin: false, isMax: false },
        { type: ACTIONS.DECREMENT },
      ),
    ).toEqual({
      columnCount: 8,
      isMin: true,
      isMax: false,
    })
  })

  it('should default', () => {
    expect(
      reducer({ columnCount: 4, isMin: false, isMax: false }, ''),
    ).toEqual({ columnCount: 4, isMin: false, isMax: false })
  })
})
