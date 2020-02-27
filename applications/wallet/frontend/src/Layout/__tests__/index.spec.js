import TestRenderer, { act } from 'react-test-renderer'

import mockUser from '../../User/__mocks__/user'

import Layout from '..'

jest.mock('../../ProjectSwitcher', () => 'ProjectSwitcher')
jest.mock('../../Sidebar', () => 'Sidebar')

const noop = () => () => {}

describe('<Layout />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(
      <Layout user={mockUser} logout={noop}>
        Hello World
      </Layout>,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Open Sidebar Menu' })
        .props.onClick({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()
  })
})
