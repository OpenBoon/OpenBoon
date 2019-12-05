import TestRenderer from 'react-test-renderer'

import Pagination from '..'

const noop = () => () => {}

describe('<Pagination />', () => {
  it('should render properly on first page', () => {
    const component = TestRenderer.create(
      <Pagination
        legend="Jobs: 1–17 of 415"
        currentPage={1}
        totalPages={2}
        prevLink="/"
        nextLink="/?page=2"
        onClick={noop}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly on last page', () => {
    const component = TestRenderer.create(
      <Pagination
        legend="Jobs: 1–17 of 415"
        currentPage={2}
        totalPages={2}
        prevLink="/"
        nextLink="/?page=2"
        onClick={noop}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
