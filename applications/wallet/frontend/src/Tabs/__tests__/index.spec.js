import TestRenderer, { act } from 'react-test-renderer'

import Tabs from '..'

jest.mock('next/link', () => 'Link')

describe('<Tabs />', () => {
  it('should prevent a click on a selected tab', () => {
    const mockFn = jest.fn()

    const component = TestRenderer.create(
      <Tabs
        tabs={[{ title: 'Selected', href: '/selected', isSelected: true }]}
      />,
    )

    act(() => {
      component.root.findByType('a').props.onClick({ preventDefault: mockFn })
    })

    expect(mockFn).toHaveBeenCalled()
  })

  it('should not prevent a click on a non-selected tab', () => {
    const mockFn = jest.fn()

    const component = TestRenderer.create(
      <Tabs tabs={[{ title: 'Not Selected', href: '/not-selected' }]} />,
    )

    act(() => {
      component.root.findByType('a').props.onClick({ preventDefault: mockFn })
    })

    expect(mockFn).not.toHaveBeenCalled()
  })
})
