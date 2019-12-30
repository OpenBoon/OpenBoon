import TestRenderer from 'react-test-renderer'

import Pagination from '..'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

describe('<Pagination />', () => {
  it('should render properly on first page', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs',
      query: { projectId: PROJECT_ID },
    })

    const component = TestRenderer.create(
      <Pagination currentPage={1} totalPages={2} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly on last page', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs',
      query: { projectId: PROJECT_ID },
    })

    const component = TestRenderer.create(
      <Pagination currentPage={2} totalPages={2} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
