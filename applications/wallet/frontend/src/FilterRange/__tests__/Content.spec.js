import { createElement as mockCreateElement } from 'react'
import TestRenderer, { act } from 'react-test-renderer'

import FilterRangeContent from '../Content'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

jest.mock('react-compound-slider', () => ({
  Slider: ({ children, ...rest }) => mockCreateElement('Slider', rest),
  Rail: () => 'Rail',
  Handles: () => 'Handles',
  Tracks: () => 'Tracks',
}))

jest.mock('../../Filters/Reset', () => 'FiltersReset')

describe('<FilterRangeContent />', () => {
  it('should render with file sizes and reset', () => {
    const filter = {
      type: 'range',
      attribute: 'source.filesize',
      values: { min: 200, max: 8000 },
    }

    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('swr').__setMockUseSWRResponse({
      data: {
        count: 584,
        results: {
          count: 580,
          min: 180,
          max: 8525.0,
          avg: 899.5689655172414,
          sum: 521750.0,
        },
      },
    })

    const component = TestRenderer.create(
      <FilterRangeContent
        projectId={PROJECT_ID}
        assetId=""
        filters={[filter]}
        filter={filter}
        filterIndex={0}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root.findByType('Slider').props.onUpdate([200, 8000])
    })

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root.findByType('Slider').props.onChange([300, 7000])
    })

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root.findByType('FiltersReset').props.onReset()
    })

    expect(component.toJSON()).toMatchSnapshot()
  })
})
