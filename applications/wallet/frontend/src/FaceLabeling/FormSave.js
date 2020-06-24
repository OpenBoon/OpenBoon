import PropTypes from 'prop-types'

const FaceLablingFormSave = ({ isChanged, isLoading }) => {
  if (isLoading) {
    return 'Saving...'
  }
  if (isChanged) {
    return 'Save'
  }
  return 'Saved'
}

FaceLablingFormSave.propTypes = {
  isChanged: PropTypes.bool.isRequired,
  isLoading: PropTypes.bool.isRequired,
}

export default FaceLablingFormSave
