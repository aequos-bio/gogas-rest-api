import { useMemo } from 'react';
import { useSelector } from 'react-redux';
import jwt_decode from 'jwt-decode';
import moment from 'moment-timezone';

const useJwt = () => {
  const authentication = useSelector((state) => state.authentication);

  const decodedJwt = useMemo(() => {
    if (authentication.jwtToken) {
      const j = jwt_decode(authentication.jwtToken);
      if (moment(j.exp * 1000).isBefore(moment())) {
        j.expired = true;
      }
      return j;
    }
    return null;
  }, [authentication]);

  return decodedJwt;
};

export default useJwt;
