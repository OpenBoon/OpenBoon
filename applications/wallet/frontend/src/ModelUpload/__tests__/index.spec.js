import TestRenderer, { act } from 'react-test-renderer'

import ModelUpload from '..'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const MODEL_ID = '621bf775-89d9-1244-9596-d6df43f1ede5'

const noop = () => () => {}

describe('<ModelUpload />', () => {
  it('should render properly', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/models/[modelId]',
      query: { projectId: PROJECT_ID, modelId: MODEL_ID },
    })

    const component = TestRenderer.create(<ModelUpload />)

    expect(component.toJSON()).toMatchSnapshot()

    // Drag enter
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Drag and Drop Zone' })
        .props.onDragEnter()
    })

    expect(component.toJSON()).toMatchSnapshot()

    // Drag leave
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Drag and Drop Zone' })
        .props.onDragLeave()
    })

    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Drag and Drop Zone' })
        .props.onDragOver({ preventDefault: noop })
    })

    act(() => {
      component.root
        .findByProps({ children: 'click to upload' })
        .props.onClick()
    })

    // Drop
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Drag and Drop Zone' })
        .props.onDrop({
          preventDefault: noop,
          dataTransfer: {
            files: [{ name: 'model.zip', size: 123456789 }],
          },
        })
    })

    expect(component.toJSON()).toMatchSnapshot()

    // Cancel
    act(() => {
      component.root.findByProps({ children: 'Cancel' }).props.onClick()
    })

    // File input
    act(() => {
      component.root
        .findByProps({ type: 'file' })
        .props.onClick({ target: { value: '' } })
    })

    act(() => {
      component.root.findByProps({ type: 'file' }).props.onChange({
        preventDefault: noop,
        target: {
          files: [{ name: 'model.zip', size: 123456789 }],
        },
      })
    })

    // Confirm
    act(() => {
      component.root
        .findByProps({ children: 'Upload Model File' })
        .props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()

    // Cancel
    act(() => {
      component.root.findByProps({ children: 'Cancel' }).props.onClick()
    })
  })
})
