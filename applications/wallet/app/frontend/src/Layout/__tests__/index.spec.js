import TestRenderer, { act } from 'react-test-renderer'

import Layout from '..'

jest.mock('../../ProjectSwitcher', () => 'ProjectSwitcher')
jest.mock('../../Sidebar', () => 'Sidebar')

const noop = () => () => {}

describe('<Layout />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(
      <Layout>{() => `Hello World`}</Layout>,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ children: 'Hamburger' })
        .props.onClick({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()
  })
})
