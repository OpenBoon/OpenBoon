import { reducer } from '../reducer'

describe('<DataVisualization /> reducer', () => {
  it('should UPDATE', () => {
    expect(
      JSON.stringify(
        reducer([{ id: 'a' }, { id: 'b', type: 'facet' }, { id: 'c' }], {
          type: 'UPDATE',
          payload: {
            updatedChart: { id: 'b', type: 'facet', attribute: 'system.type' },
            chartIndex: 1,
          },
        }),
      ),
    ).toEqual(
      JSON.stringify([
        { id: 'a' },
        { id: 'b', type: 'facet', attribute: 'system.type' },
        { id: 'c' },
      ]),
    )
  })

  it('should DELETE', () => {
    expect(
      JSON.stringify(
        reducer([{ id: 'a' }, { id: 'b', type: 'facet' }, { id: 'c' }], {
          type: 'DELETE',
          payload: {
            chartIndex: 1,
          },
        }),
      ),
    ).toEqual(JSON.stringify([{ id: 'a' }, { id: 'c' }]))
  })

  it('should default', () => {
    expect(reducer([], {})).toEqual([])
  })
})
