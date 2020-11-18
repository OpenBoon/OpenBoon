import TestRenderer, { act } from 'react-test-renderer'

import PaginationPage from '../Page'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

const noop = () => () => {}

describe('<PaginationPage />', () => {
  it('should render properly', () => {
    const mockSelectFn = jest.fn()
    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs',
      query: { projectId: PROJECT_ID },
    })

    const component = TestRenderer.create(
      <PaginationPage currentPage={1} totalPages={456} />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Go to page' })
        .props.onFocus({ target: { select: mockSelectFn } })
    })

    expect(mockSelectFn).toHaveBeenCalledWith()

    act(() => {
      component.root.findByProps({ 'aria-label': 'Go to page' }).props.onBlur()
    })

    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Go to page' })
        .props.onChange({ target: { value: 123 } })
    })

    act(() => {
      component.root.findByType('form').props.onSubmit({ preventDefault: noop })
    })

    expect(mockRouterPush).toHaveBeenCalledWith(
      '/[projectId]/jobs?page=123',
      `/${PROJECT_ID}/jobs?page=123`,
    )
  })
})
