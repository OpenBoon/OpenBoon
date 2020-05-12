import TestRenderer from 'react-test-renderer'

import FiltersTextContent from '..'

const noop = () => () => {}

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

jest.mock('../../Filters/Reset', () => 'FiltersReset')

describe('<FiltersTextContent />', () => {
  it('should render the "Text Content" filter', () => {
    const mockRouterPush = jest.fn()
    require('next/router').__setMockPushFunction(mockRouterPush)

    const component = TestRenderer.create(
      <FiltersTextContent
        projectId={PROJECT_ID}
        assetId=""
        filter={{
          type: 'textContent',
          attribute: 'analysis.zvi-text-detection',
          values: {
            query: 'Cat',
          },
        }}
        filters={[]}
        setIsMenuOpen={noop}
        filterIndex={0}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    component.root.findByProps({ children: 'delete' }).props.onClick()

    expect(mockRouterPush).toHaveBeenCalledWith(
      {
        pathname: '/[projectId]/visualizer',
        query: {
          query: '',
          projectId: '76917058-b147-4556-987a-0a0f11e46d9b',
          id: '',
        },
      },
      '/76917058-b147-4556-987a-0a0f11e46d9b/visualizer',
    )
  })
})
