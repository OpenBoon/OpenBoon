import TestRenderer, { act } from 'react-test-renderer'

import ProjectSwitcher from '..'
import ProjectSwitcherDropDown from '../DropDown'

import projects from '../__mocks__/projects'

const noop = () => () => {}

describe('<ProjectSwitcher />', () => {
  it('should render properly with data', () => {
    const mockFn = jest.fn()
    const component = TestRenderer.create(
      <ProjectSwitcher onClick={mockFn}>
        <ProjectSwitcherDropDown projects={projects.list} onSelect={mockFn} />
      </ProjectSwitcher>,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByType('button')
        .props.onClick({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ children: 'Zorroa EasyAs123' })
        .props.onClick({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()
  })
})
