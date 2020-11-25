import { INITIAL_STATE, reducer } from '../reducer'

describe('<ModelMatrix /> reducer', () => {
  it('should save values', () => {
    expect(reducer(INITIAL_STATE, { width: 123 })).toEqual({
      ...INITIAL_STATE,
      width: 123,
    })
  })
})
