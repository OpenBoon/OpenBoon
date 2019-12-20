import TestRenderer, { act } from 'react-test-renderer'

import Pagination from '..'

describe('<Pagination />', () => {
  it('should render properly on first page', () => {
    const mockOnClickFn = jest.fn()
    const component = TestRenderer.create(
      <Pagination
        legend="Jobs: 1–17 of 415"
        currentPage={1}
        totalPages={2}
        prevLink="/"
        nextLink="/?page=2"
        onClick={mockOnClickFn}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root.findByProps({ rel: 'next' }).props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()
    expect(mockOnClickFn).toHaveBeenCalledWith({ newPage: 2 })
  })

  it('should render properly on last page', () => {
    const mockOnClickFn = jest.fn()
    const component = TestRenderer.create(
      <Pagination
        legend="Jobs: 1–17 of 415"
        currentPage={2}
        totalPages={2}
        prevLink="/"
        nextLink="/?page=2"
        onClick={mockOnClickFn}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root.findByProps({ rel: 'prev' }).props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()
    expect(mockOnClickFn).toHaveBeenCalledWith({ newPage: 1 })
  })
})
