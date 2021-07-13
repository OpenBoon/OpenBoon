import TestRenderer, { act } from 'react-test-renderer'

import matrix from '../__mocks__/matrix'

import mockUser from '../../User/__mocks__/user'

import User from '../../User'

import ModelMatrix from '..'

jest.mock('../Preview', () => 'ModelMatrixPreview')

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const MODEL_ID = '621bf775-89d9-1244-9596-d6df43f1ede5'

const noop = () => () => {}

describe('<ModelMatrix />', () => {
  it('should render properly when out of date', async () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/models/[modelId]/matrix',
      query: {
        projectId: PROJECT_ID,
        modelId: MODEL_ID,
      },
    })

    require('swr').__setMockUseSWRResponse({
      data: { ...matrix, unappliedChanges: true },
    })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <ModelMatrix />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly', async () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/models/[modelId]/matrix',
      query: {
        projectId: PROJECT_ID,
        modelId: MODEL_ID,
      },
    })

    require('swr').__setMockUseSWRResponse({
      data: matrix,
    })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <ModelMatrix />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()

    // Hide Minimap
    act(() => {
      component.root.findByProps({ 'aria-label': 'Mini map' }).props.onClick()
    })

    // Does nothing since zoom = 1 = min
    act(() => {
      component.root.findByProps({ 'aria-label': 'Zoom Out' }).props.onClick()
    })

    // Zoom 2x
    act(() => {
      component.root.findByProps({ 'aria-label': 'Zoom In' }).props.onClick()
    })

    // Select a cell
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'pink / pink: 0%' })
        .props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()

    // Back to zoom 1x
    act(() => {
      component.root.findByProps({ 'aria-label': 'Zoom Out' }).props.onClick()
    })

    // Change view to Absolute
    act(() => {
      component.root
        .findByProps({ type: 'radio', value: 'absolute' })
        .props.onClick()
    })

    act(() => {
      component.root.findByType('form').props.onSubmit({ preventDefault: noop })
    })

    // Select a different cell
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'pink / red: 0' })
        .props.onClick()
    })

    // Close panel
    act(() => {
      component.root.findByProps({ 'aria-label': 'Preview' }).props.onClick()
    })

    // Deselect a cell
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'pink / red: 0' })
        .props.onClick()
    })

    // Reselect cell
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'pink / red: 0' })
        .props.onClick()
    })

    // Deselect cell
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'pink / red: 0' })
        .props.onClick()
    })

    // Hover label
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Prediction Label: pink' })
        .props.onMouseEnter()
    })

    // Hover label
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Prediction Label: pink' })
        .props.onMouseLeave()
    })
  })
})
