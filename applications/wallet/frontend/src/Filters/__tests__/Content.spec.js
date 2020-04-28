import TestRenderer, { act } from 'react-test-renderer'

import aggregate from '../../FilterFacet/__mocks__/aggregate'

import FiltersContent from '../Content'

const noop = () => () => {}

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

describe('<FiltersContent />', () => {
  it('should render the "Exists" filter', () => {
    const filters = [{ attribute: 'location.point', type: 'exists' }]

    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    const component = TestRenderer.create(
      <FiltersContent
        projectId={PROJECT_ID}
        assetId=""
        filters={filters}
        setIsMenuOpen={noop}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    // click "Missing"
    act(() => {
      component.root
        .findByProps({ children: 'Missing' })
        .props.onClick({ preventDefault: noop })
    })

    expect(mockRouterPush).toHaveBeenCalledWith(
      {
        pathname: '/[projectId]/visualizer',
        query: {
          projectId: '76917058-b147-4556-987a-0a0f11e46d9b',
          id: '',
          filters:
            '[{"attribute":"location.point","type":"exists","values":{"exists":false}}]',
        },
      },
      '/76917058-b147-4556-987a-0a0f11e46d9b/visualizer?filters=[{"attribute":"location.point","type":"exists","values":{"exists":false}}]',
    )
  })

  it('should render the "Facet" filter', () => {
    const filters = [{ attribute: 'location.city', type: 'facet' }]

    require('swr').__setMockUseSWRResponse({ data: aggregate })

    const component = TestRenderer.create(
      <FiltersContent
        projectId={PROJECT_ID}
        assetId=""
        filters={filters}
        setIsMenuOpen={noop}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
