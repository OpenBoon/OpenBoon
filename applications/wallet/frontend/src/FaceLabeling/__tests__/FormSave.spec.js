import TestRenderer from 'react-test-renderer'
import FaceLabelingFormSave from '../FormSave'

describe('<FaceLabelingFormSave />', () => {
  it('should render when isLoading', () => {
    const component = TestRenderer.create(
      <FaceLabelingFormSave isChanged isLoading />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
