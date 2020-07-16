import { typography } from '../Styles'

import NoJobsSvg from '../Icons/noJobs.svg'

const ModelsEmpty = () => {
  return (
    <div
      css={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        fontSize: typography.size.large,
        lineHeight: typography.height.large,
      }}
    >
      <NoJobsSvg width={200} />
      <div>There are currently no jobs in the queue.</div>
      <div>Any new job will appear here.</div>
    </div>
  )
}

export default ModelsEmpty
