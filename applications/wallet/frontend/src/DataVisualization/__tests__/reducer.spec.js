import { reducer } from '../reducer'

describe('<DataVisualization /> reducer', () => {
  it('should default', () => {
    expect(reducer([], {})).toEqual([])
  })
})
