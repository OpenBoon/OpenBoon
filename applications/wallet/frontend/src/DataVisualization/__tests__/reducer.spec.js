import { reducer } from '../reducer'

describe('<DataVisualization /> reducer', () => {
  it('should UPDATE', () => {
    expect(
      JSON.stringify(
        reducer([{ id: 1 }, { id: 2, type: 'facet' }, { id: 3 }], {
          type: 'UPDATE',
          payload: {
            updatedChart: { id: 2, type: 'facet', attribute: 'system.type' },
            chartIndex: 1,
          },
        }),
      ),
    ).toEqual(
      JSON.stringify([
        { id: 1 },
        { id: 2, type: 'facet', attribute: 'system.type' },
        { id: 3 },
      ]),
    )
  })

  it('should DELETE', () => {
    expect(
      JSON.stringify(
        reducer([{ id: 1 }, { id: 2, type: 'facet' }, { id: 3 }], {
          type: 'DELETE',
          payload: {
            chartIndex: 1,
          },
        }),
      ),
    ).toEqual(JSON.stringify([{ id: 1 }, { id: 3 }]))
  })

  it('should default', () => {
    expect(reducer([], {})).toEqual([])
  })
})
