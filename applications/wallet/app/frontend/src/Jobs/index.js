import PropTypes from 'prop-types'
import useSWR from 'swr'

const Jobs = ({ selectedProject, children }) => {
  const { data: { list } = {} } = useSWR(
    `api/v1/projects/${selectedProject.id}/jobs/`,
  )

  if (!Array.isArray(list)) return 'Loading...'

  if (list.length === 0) return 'You have 0 jobs'

  return children({ jobs: list })
}

Jobs.propTypes = {
  selectedProject: PropTypes.shape({
    id: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
  }).isRequired,
  children: PropTypes.func.isRequired,
}

export default Jobs
